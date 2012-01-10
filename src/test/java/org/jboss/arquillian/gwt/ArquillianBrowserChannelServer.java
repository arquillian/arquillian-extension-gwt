package org.jboss.arquillian.gwt;

import java.io.IOException;
import java.net.Socket;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.BrowserChannelServer;

public class ArquillianBrowserChannelServer extends BrowserChannelServer {

    public ArquillianBrowserChannelServer(Socket socket, boolean ignoreRemoteDeath) throws IOException {
        this(new ConsoleTreeLogger(), socket, (SessionHandlerServer) new ArquillianGwtSessionHandler(), ignoreRemoteDeath);
    }

    public ArquillianBrowserChannelServer(TreeLogger initialLogger, Socket socket, SessionHandlerServer handler,
            boolean ignoreRemoteDeath) throws IOException {
        super(initialLogger, socket, handler, ignoreRemoteDeath);
    }

}
