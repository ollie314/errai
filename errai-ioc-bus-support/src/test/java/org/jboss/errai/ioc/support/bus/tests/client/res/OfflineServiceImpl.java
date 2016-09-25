package org.jboss.errai.ioc.support.bus.tests.client.res;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.errai.bus.server.annotations.ShadowService;

@Singleton
@ShadowService
public class OfflineServiceImpl implements OfflineService, OnlineService {

  @Inject
  private Greeter greeter;
  
  @Override
  public String greeting() {
    return greeter.offline();
  }

}
