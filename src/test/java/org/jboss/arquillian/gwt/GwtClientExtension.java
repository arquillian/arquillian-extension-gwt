package org.jboss.arquillian.gwt;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class GwtClientExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(GwtClientEnvironment.class);
    }
}
