package org.jboss.arquillian.gwt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.ModuleHandle;

public class ArquillianModuleHandle implements ModuleHandle {

    @Override
    public TreeLogger getLogger() {
        return new ConsoleTreeLogger();
    }

    @Override
    public void unload() {
        // do-nothing
    }

}
