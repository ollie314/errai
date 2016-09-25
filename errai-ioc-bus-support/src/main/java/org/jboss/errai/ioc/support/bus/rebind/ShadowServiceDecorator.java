/*
 * Copyright (C) 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.support.bus.rebind;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.ClientMessageBus;
import org.jboss.errai.bus.client.api.base.MessageBuilder;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;
import org.jboss.errai.bus.server.annotations.Remote;
import org.jboss.errai.bus.server.annotations.ShadowService;
import org.jboss.errai.codegen.Parameter;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.VariableReference;
import org.jboss.errai.codegen.builder.AnonymousClassStructureBuilder;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ElseBlockBuilder;
import org.jboss.errai.codegen.builder.impl.ObjectBuilder;
import org.jboss.errai.codegen.util.EmptyStatement;
import org.jboss.errai.codegen.util.If;
import org.jboss.errai.codegen.util.ProxyUtil;
import org.jboss.errai.codegen.util.Refs;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.errai.ioc.client.api.CodeDecorator;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCDecoratorExtension;
import org.jboss.errai.ioc.rebind.ioc.injector.api.Decorable;
import org.jboss.errai.ioc.rebind.ioc.injector.api.FactoryController;

/**
 * Generates logic to register client-side shadow services for Errai's message
 * bus. Shadow services are used when:
 * <ul>
 * <li>Remote communication is turned off
 * <li>Errai's message bus is not in connected state
 * <li>A remote endpoint for the service doesn't exist
 * </ul>
 * 
 * @author Mike Brock
 * @author Christian Sadilek <csadilek@redhat.com>
 */
@CodeDecorator
public class ShadowServiceDecorator extends IOCDecoratorExtension<ShadowService> {
  public ShadowServiceDecorator(Class<ShadowService> decoratesWith) {
    super(decoratesWith);
  }

  @Override
  public void generateDecorator(final Decorable decorable, final FactoryController controller) {
    final ShadowService shadowService = (ShadowService) decorable.getAnnotation();
    String serviceName = null;

    Statement subscribeShadowStatement = null;
    final Class<?> javaClass = decorable.getType().asClass();
    for (final Class<?> intf : javaClass.getInterfaces()) {
      if (intf.isAnnotationPresent(Remote.class)) {
        serviceName = intf.getName() + ":RPC";

        final AnonymousClassStructureBuilder builder = generateMethodDelegates(intf, decorable, controller);
        subscribeShadowStatement = Stmt.castTo(ClientMessageBus.class, Stmt.invokeStatic(ErraiBus.class, "get"))
                .invoke("subscribeShadow", serviceName, builder.finish());
      }

      if (serviceName == null) {
        if (shadowService.value().equals("")) {
          serviceName = decorable.getName();
        }
        else {
          serviceName = shadowService.value();
        }

        subscribeShadowStatement = Stmt.castTo(ClientMessageBus.class, Stmt.invokeStatic(ErraiBus.class, "get"))
                .invoke("subscribeShadow", serviceName, controller.contextGetInstanceStmt());
      }

      controller.addFactoryInitializationStatements(Collections.singletonList(subscribeShadowStatement));
    }
  }

  private AnonymousClassStructureBuilder generateMethodDelegates(final Class<?> intf, final Decorable decorable, final FactoryController controller) {

    final BlockBuilder<AnonymousClassStructureBuilder> builder = ObjectBuilder.newInstanceOf(MessageCallback.class)
            .extend().publicOverridesMethod("callback", Parameter.of(Message.class, "message"))
            .append(Stmt.declareVariable("commandType", String.class,
                    Stmt.loadVariable("message").invoke("getCommandType")))
            .append(Stmt.declareVariable("methodParms", List.class,
                    Stmt.loadVariable("message").invoke("get", List.class, Stmt.loadLiteral("MethodParms"))));

    for (final Method method : intf.getMethods()) {
      if (ProxyUtil.isMethodInInterface(intf, method)) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final VariableReference[] objects = new VariableReference[parameterTypes.length];
        final BlockBuilder<ElseBlockBuilder> blockBuilder = If
                .cond(Stmt.loadLiteral(ProxyUtil.createCallSignature(intf, method)).invoke("equals",
                        Stmt.loadVariable("commandType")));
        
        for (int i = 0; i < parameterTypes.length; i++) {
          final Class<?> parameterType = parameterTypes[i];
          blockBuilder.append(Stmt.declareVariable("var" + i, parameterType,
                  Stmt.castTo(parameterType, Stmt.loadVariable("methodParms").invoke("get", i))));
          objects[i] = Refs.get("var" + i);
        }

        final boolean hasReturnType = !method.getReturnType().equals(void.class);
        blockBuilder.append(Stmt.declareFinalVariable("instance", intf, controller.contextGetInstanceStmt()));
        final Statement methodInvocation = Stmt.nestedCall(Stmt.loadVariable("instance")).invoke(method.getName(), (Object[]) objects);
        blockBuilder.append(Stmt.try_()
                .append((hasReturnType) ? Stmt.declareFinalVariable("ret", method.getReturnType(), methodInvocation) : methodInvocation)
                .append((decorable.isEnclosingTypeDependent()) ? Stmt.loadVariable("context").invoke("destroyInstance", Refs.get("instance")) 
                        : EmptyStatement.INSTANCE)
                .append((hasReturnType) ? Stmt.invokeStatic(MessageBuilder.class, "createConversation", Stmt.loadVariable("message"))
                        .invoke("subjectProvided").invoke("with", "MethodReply", Refs.get("ret"))
                        .invoke("noErrorHandling").invoke("sendNowWith", Stmt.invokeStatic(ErraiBus.class, "get"))
                        : EmptyStatement.INSTANCE)
                .finish().catch_(Throwable.class, "throwable")
                .append(Stmt.throw_(RuntimeException.class, Stmt.loadVariable("throwable"))).finish());
        builder.append(blockBuilder.finish());
      }
    }
    return builder.finish();
  }
}
