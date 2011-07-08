package org.jboss.errai.ioc.rebind.ioc.codegen;

import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.LoadClassReference;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaClassFactory;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class Cast implements Statement {
  private MetaClass toType;
  private Statement statement;

  private Cast(MetaClass toType, Statement statement) {
    this.toType = toType;
    this.statement = statement;
  }

  public static Cast to(Class<?> cls, Statement stmt) {
     return to(MetaClassFactory.get(cls), stmt);
  }

  public static Cast to(MetaClass cls, Statement stmt) {
    return new Cast(cls, stmt);
  }

  @Override
  public String generate(Context context) {
    return "(" + LoadClassReference.getClassReference(toType, context) + ") " + statement.generate(context);
  }

  @Override
  public MetaClass getType() {
    return toType;
  }
}
