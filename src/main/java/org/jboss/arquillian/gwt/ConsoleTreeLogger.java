package org.jboss.arquillian.gwt;

import com.google.gwt.core.ext.TreeLogger;

public class ConsoleTreeLogger extends TreeLogger {
    @Override
    public void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
        if (isLoggable(type)) {
            System.out.println(msg);
        }

    }

    @Override
    public boolean isLoggable(Type type) {
        switch (type) {
            case ALL:
            case TRACE:
            case DEBUG:
            case SPAM:
                return false;
            case INFO:
            case WARN:
            case ERROR:
                return true;
        }

        return false;
    }

    @Override
    public TreeLogger branch(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
        return this;
    }
}
