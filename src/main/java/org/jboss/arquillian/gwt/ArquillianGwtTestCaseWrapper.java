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

package org.jboss.arquillian.gwt;

import com.google.gwt.junit.JUnitShell.Strategy;
import com.google.gwt.junit.PropertyDefiningStrategy;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class ArquillianGwtTestCaseWrapper extends GWTTestCase {

  private String moduleName;
  private String methodName;
  private Class<?> testClass;
  
  public ArquillianGwtTestCaseWrapper() {}
  
  public ArquillianGwtTestCaseWrapper(String moduleName, String methodName, Class<?> testClass) {
    this.moduleName = moduleName;
    this.methodName = methodName;
    this.testClass = testClass;
    
    TestModuleInfo moduleInfo = new TestModuleInfo(getModuleName(), getSyntheticModuleName(), getStrategy());
    ALL_GWT_TESTS.put(getSyntheticModuleName(), moduleInfo);
    moduleInfo.getTests().add(new TestInfo(getSyntheticModuleName(), testClass.getName(), methodName));
  }
  
  @Override
  public String getModuleName() {
    return moduleName;
  }

  @Override
  public String getName() {
    return methodName;
  }

  @Override
  public Strategy getStrategy() {
    return new PropertyDefiningStrategy(this, testClass);
  }
}
