package org.jboss.arquillian.gwt.example.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface ShadowmanBundle extends ClientBundle {

    @Source("org/jboss/arquillian/gwt/example/shadowman.png")
    ImageResource shadowman();

}
