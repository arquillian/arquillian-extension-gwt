package org.jboss.arquillian.gwt;

import java.io.IOException;
import java.net.Socket;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.shell.ModuleSpaceOOPHM;

public class ArquillianJavaScriptHost extends ModuleSpaceOOPHM {

    public ArquillianJavaScriptHost(String moduleName, Socket socket, boolean ignoreRemoteDeath)
            throws UnableToCompleteException, IOException {
        super(new ArquillianBrowserWidgetHost().createModuleSpaceHost(new ArquillianModuleHandle(), moduleName), moduleName,
                new ArquillianBrowserChannelServer(socket, ignoreRemoteDeath));
    }

}
