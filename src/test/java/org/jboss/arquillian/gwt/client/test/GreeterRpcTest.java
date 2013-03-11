/*
 * Copyright 2013 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.arquillian.gwt.client.test;

import java.io.File;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.gwt.RunAsGwtClient;
import org.jboss.arquillian.gwt.client.ArquillianGwtTestCase;
import org.jboss.arquillian.gwt.client.GreetingService;
import org.jboss.arquillian.gwt.client.GreetingServiceAsync;
import org.jboss.arquillian.gwt.client.shared.Greeter;
import org.jboss.arquillian.gwt.server.GreetingServiceImpl;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

@RunWith(Arquillian.class)
public class GreeterRpcTest extends ArquillianGwtTestCase {

  @Inject
  Greeter greeter;

  @Deployment
  public static WebArchive createDeployment() {
    return ShrinkWrap.create(WebArchive.class, "test.war")
      .addClass(Greeter.class)
      .addClass(GreetingService.class)
      .addClass(GreetingServiceImpl.class)
      .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"))
      .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
  }

  @Test
  public void testCreateGreeting() {
    assertEquals("Hello, Earthling!", greeter.createGreeting("Earthling"));
  }

  @Test
  @RunAsGwtClient(moduleName = "org.jboss.arquillian.gwt.TestModule")
  public void testGreetingService() {
    GreetingServiceAsync greetingService = GWT.create(GreetingService.class);
    greetingService.greetServer("Hello, Earthling!", new AsyncCallback<String>() {
      @Override
      public void onFailure(Throwable caught) {
        Assert.fail("Request failure: " + caught.getMessage());
      }

      @Override
      public void onSuccess(String result) {
        assertEquals("Received invalid response from Server", "Howdy!", result);
        finishTest();
      }
    });
    delayTestFinish(5000);
  }

}