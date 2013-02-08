/*
 * Copyright 2011 JBoss, by Red Hat, Inc
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

import org.jboss.arquillian.gwt.client.ArquillianGwtTestCase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import com.google.gwt.junit.client.GWTTestCase.BaseStrategy;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 */
// TODO leverage Arquillian Archive SPI rather than creating our own Archive wrapper.
public class GwtArchive {

  private static final String WAR_DIR = "war";

  public static WebArchive get() {
    File war = new File(WAR_DIR);
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
        WebArchive archive = ShrinkWrap.create(WebArchive.class, module + ".war")
            .addClass(ArquillianJunitHostImpl.class)
            .addClass(ArquillianJunitMessageQueue.class)
            .addClass(ArquillianGwtTestCase.class)
            .addAsLibraries(
                DependencyResolvers.use(MavenDependencyResolver.class)
                    .artifact("com.google.gwt:gwt-user:2.5.0")
                    .artifact("com.google.gwt:gwt-dev:2.5.0")
                    .artifact("com.google.gwt:gwt-servlet:2.5.0")
                    .resolveAsFiles())
            .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"))
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        for (File aFile : moduleDir.listFiles()) {
          archive.addAsWebResource(aFile, aFile.getName());
        }

        return archive;
      }
    }

    throw new RuntimeException("No GWT module found!");
  }
}