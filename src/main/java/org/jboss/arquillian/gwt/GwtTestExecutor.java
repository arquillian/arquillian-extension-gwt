package org.jboss.arquillian.gwt;

import java.lang.reflect.Method;

import junit.framework.TestFailure;

import org.jboss.arquillian.container.test.impl.execution.event.ExecutionEvent;
import org.jboss.arquillian.container.test.impl.execution.event.LocalExecutionEvent;
import org.jboss.arquillian.container.test.impl.execution.event.RemoteExecutionEvent;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.gwt.client.ArquillianGwtTestCase;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.TestResult.Status;
import org.jboss.arquillian.test.spi.annotation.TestScoped;

import com.google.gwt.junit.JUnitShell;

/**
 * Arquillian test executor for GWT tests. Executes test methods annotated with {@link RunAsGwtClient}.
 * 
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class GwtTestExecutor {

  @Inject
  @TestScoped
  private InstanceProducer<TestResult> testResult;

  public void onRemoteTestExecution(@Observes final EventContext<RemoteExecutionEvent> eventContext) {
    maybeRunGwtTest(eventContext);
  }

  public void onLocalTestExecution(@Observes final EventContext<LocalExecutionEvent> eventContext) {
    maybeRunGwtTest(eventContext);
  }

  private void maybeRunGwtTest(EventContext<? extends ExecutionEvent> eventContext) {
    final Method testMethod = eventContext.getEvent().getExecutor().getMethod();
    RunAsGwtClient runAsGwtClient = testMethod.getDeclaringClass().getAnnotation(RunAsGwtClient.class);
    if (runAsGwtClient == null) {
      runAsGwtClient = testMethod.getAnnotation(RunAsGwtClient.class);
    }
    
    if (runAsGwtClient != null) {
      runGwtTest(testMethod.getDeclaringClass(), testMethod.getName(), runAsGwtClient.moduleName());
    }
    else {
      eventContext.proceed();
    }
  }

  private void runGwtTest(final Class<?> testClass, final String methodName, final String moduleName) {
    ArquillianGwtTestCase gwtTest = new ArquillianGwtTestCase(moduleName, methodName, testClass);
    
    TestResult arquillianTestResult = new TestResult();
    junit.framework.TestResult junitTestResult = new junit.framework.TestResult();

    try {
      JUnitShell.runTest(gwtTest, testClass, junitTestResult);
      
      if (junitTestResult.wasSuccessful()) {
        arquillianTestResult.setStatus(Status.PASSED);
      }
      else {
        if (junitTestResult.failures().hasMoreElements()) {
          TestFailure f = junitTestResult.failures().nextElement();
          arquillianTestResult.setThrowable(f.thrownException());
        }

        if (junitTestResult.errors().hasMoreElements()) {
          TestFailure f = junitTestResult.errors().nextElement();
          arquillianTestResult.setThrowable(f.thrownException());
        }
        arquillianTestResult.setStatus(Status.FAILED);
      }
    }
    catch (Throwable e) {
      arquillianTestResult.setThrowable(e);
      arquillianTestResult.setStatus(Status.FAILED);
    }
    finally {
      arquillianTestResult.setEnd(System.currentTimeMillis());
      testResult.set(arquillianTestResult);
    }
  }
}