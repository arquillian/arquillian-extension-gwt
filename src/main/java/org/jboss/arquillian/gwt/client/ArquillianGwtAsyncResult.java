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

package org.jboss.arquillian.gwt.client;

import com.google.gwt.user.client.Timer;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 * 
 * @param <R>
 */
public class ArquillianGwtAsyncResult<R> {

  private final int timeoutMillis;
  private boolean resultAvailable;
  private R result;

  public ArquillianGwtAsyncResult(int timeoutMillis) {
    this.timeoutMillis = timeoutMillis;
  }

  public R get() {
    Timer t = new Timer() { 
      @Override
      public void run() {
        
      } 
    }; 

    if (!resultAvailable) {
      throw new RuntimeException("No result after " + timeoutMillis + "ms");
    }
    return result; 
  }

  public void set(R result) {
    this.result = result;
    this.resultAvailable = true;
  }
}
