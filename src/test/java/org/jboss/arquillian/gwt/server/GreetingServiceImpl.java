package org.jboss.arquillian.gwt.server;

import org.jboss.arquillian.gwt.client.GreetingService;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements
    GreetingService {

  @Override
  public String greetServer(String input) throws IllegalArgumentException {
    return "Howdy!";
  }
}
