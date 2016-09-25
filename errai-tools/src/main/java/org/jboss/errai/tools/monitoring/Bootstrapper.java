/*
 * Copyright (C) 2011 Red Hat, Inc. and/or its affiliates.
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

package org.jboss.errai.tools.monitoring;

import org.jboss.errai.bus.client.api.messaging.MessageBus;

public class Bootstrapper {
  private MessageBus bus;
  private ActivityProcessor processor;
  private MainMonitorGUI mainMonitorGUI;
  private Dataservice dataservice;


  public Bootstrapper(ActivityProcessor processor, MessageBus bus) {
    System.setProperty("apple.laf.useScreenMenuBar", "true");

    this.bus = bus;
    this.processor = processor;
    dataservice = new Dataservice();

    mainMonitorGUI = new MainMonitorGUI(dataservice, bus);
  }

  public void init() {
    dataservice.attach(processor);
    mainMonitorGUI.attach(processor);

    mainMonitorGUI.setVisible(true);
  }
}
