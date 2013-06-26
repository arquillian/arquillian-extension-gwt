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

import java.io.File;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.gwt.client.ArquillianGwtTestCase;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import com.google.gwt.junit.client.GWTTestCase.BaseStrategy;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class GwtApplicationArchiveProcessor implements ApplicationArchiveProcessor {

  private static final String WAR = "war";

  @Override
  public void process(Archive<?> archive, TestClass testClass) {
    if (archive.getName().endsWith(WAR)) {
      WebArchive webArchive = (WebArchive) archive;
      File war = new File(WAR);
      File[] warFiles = war.listFiles();

      File moduleDir = null;
      String module = "";
      if (warFiles != null) {
        for (File f : warFiles) {
          if (f.getName().endsWith(new BaseStrategy().getSyntheticModuleExtension())) {
            moduleDir = f;
            module = moduleDir.getPath().substring(moduleDir.getPath().lastIndexOf("/") + 1);
          }
        }

        if (!module.isEmpty()) {
          webArchive
              .addClass(ArquillianJunitHostImpl.class)
              .addClass(ArquillianJunitMessageQueue.class)
              .addClass(ArquillianGwtTestCase.class)
              .addAsLibraries(
                  DependencyResolvers.use(MavenDependencyResolver.class)
                      .artifact("com.google.gwt:gwt-user:2.5.0")
                      .artifact("com.google.gwt:gwt-dev:2.5.0")
                      .artifact("com.google.gwt:gwt-servlet:2.5.0")
                      .resolveAsFiles());

          addFiles(moduleDir, webArchive , "");
          
          webArchive.addAsWebResource(getClass().getResource("arquillian-gwt-devmode.html"), "arquillian-gwt-devmode.html");
          webArchive.addAsWebResource(getClass().getResource("arquillian-gwt.html"), "arquillian-gwt.html");

          System.out.println("webArchive = " + webArchive.toString(true));
          return;
        }
      }
      
      throw new RuntimeException("No GWT module found!");
    }
  }
  
  private void addFiles(File dir, WebArchive webArchive, String path) {
    if (!path.isEmpty() && !path.endsWith(File.separator)) {
      path += File.separator;
    }
    
    for (File f : dir.listFiles()) {
      if (f.isDirectory()) {
        addFiles(f, webArchive, path + f.getName());
      } 
      else {
        webArchive.addAsWebResource(f, path + f.getName());
      }
    }
  }
}
