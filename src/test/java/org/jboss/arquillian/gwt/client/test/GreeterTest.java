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

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.gwt.RunAsGwtClient;
import org.jboss.arquillian.gwt.client.GreetingService;
import org.jboss.arquillian.gwt.client.shared.Greeter;
import org.jboss.arquillian.gwt.server.GreetingServiceImpl;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.TextBox;

@RunWith(Arquillian.class)
public class GreeterTest {

  @Inject
  Greeter greeter;

  @Deployment
  public static WebArchive createDeployment() {
    return ShrinkWrap.create(WebArchive.class, "test.war")
        .addClass(Greeter.class)
        .addClass(GreetingService.class)
        .addClass(GreetingServiceImpl.class)
        .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
  }

  @Test
  public void testCreateGreeting() {
    assertEquals("Hello, Earthling!", greeter.createGreeting("Earthling"));
    
  }

  @Test
  @RunAsGwtClient(moduleName = "org.jboss.arquillian.gwt.TestModule")
  public void testCopyTextBoxValues() {
    final TextBox textBox1 = new TextBox();
    final TextBox textBox2 = new TextBox();
    textBox1.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> newValueEvent) {
        textBox2.setText(newValueEvent.getValue());
      }
    });
    textBox1.setValue("Hello, Earthling!", true);
    assertEquals("TextBox values do not match!", textBox1.getText(), textBox2.getText());
  }

}