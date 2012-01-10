package org.jboss.arquillian.gwt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWTBridge;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.shell.GWTBridgeImpl;
import com.google.gwt.dev.shell.JavaScriptHost;

public class GwtClientEnvironment {

    @ClassScoped
    @Inject
    private InstanceProducer<ArquillianJavaScriptHost> hostInstance;

    public void initializeClientGwt(@Observes BeforeClass event) {
        RunAsGwtClient runAsGwtClient = event.getTestClass().getAnnotation(RunAsGwtClient.class);

        if (runAsGwtClient != null) {
            String moduleName = runAsGwtClient.moduleName();
            registerGwtBridge(moduleName);
        }
    }

    private void registerGwtBridge(String moduleName) {
        try {
            Method registerMethod = GWT.class.getDeclaredMethod("setBridge", GWTBridge.class);
            registerMethod.setAccessible(true);

            Socket socket = new Socket("localhost", 9997);

            ArquillianJavaScriptHost host = new ArquillianJavaScriptHost(moduleName, socket, true);

            hostInstance.set(host);

            GWTBridge bridge = new GWTBridgeImpl(host);
            registerMethod.invoke(null, bridge);
            JavaScriptHost.setHost(host);

        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to register GWT Client Bridge", e);
        } catch (UnableToCompleteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to register GWT Client Bridge", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to register GWT Client Bridge", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Unable to register GWT Client Bridge", e);
        }

    }
}
