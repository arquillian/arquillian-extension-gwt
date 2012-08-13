package org.jboss.arquillian.gwt;

import com.google.gwt.dev.shell.CompilingClassLoader;
import com.google.gwt.dev.shell.JsValueOOPHM;
import com.google.gwt.dev.shell.ServerObjectsTable;
import com.google.gwt.dev.shell.BrowserChannel.JavaObjectRef;
import com.google.gwt.dev.shell.BrowserChannel.Value;
import com.google.gwt.dev.shell.JsValue.DispatchObject;

public class BrowserChannelServerUtil {

    /**
     * Convert a JsValue into a BrowserChannel Value.
     * 
     * @param localObjects lookup table for local objects -- may be null if jsval is known to be a primitive (including String).
     * @param jsval value to convert
     * @return jsval as a Value object.
     */
    public static Value convertFromJsValue(ServerObjectsTable localObjects, JsValueOOPHM jsval) {
        Value value = new Value();
        if (jsval.isNull()) {
            value.setNull();
        } else if (jsval.isUndefined()) {
            value.setUndefined();
        } else if (jsval.isBoolean()) {
            value.setBoolean(jsval.getBoolean());
        } else if (jsval.isInt()) {
            value.setInt(jsval.getInt());
        } else if (jsval.isNumber()) {
            value.setDouble(jsval.getNumber());
        } else if (jsval.isString()) {
            value.setString(jsval.getString());
        } else if (jsval.isJavaScriptObject()) {
            value.setJsObject(jsval.getJavascriptObject());
        } else if (jsval.isWrappedJavaObject()) {
            assert localObjects != null;
            DispatchObject javaObj = jsval.getJavaObjectWrapper();
            value.setJavaObject(new JavaObjectRef(localObjects.add(javaObj)));
        } else if (jsval.isWrappedJavaFunction()) {
            assert localObjects != null;
            value.setJavaObject(new JavaObjectRef(localObjects.add(jsval.getWrappedJavaFunction())));
        } else {
            throw new RuntimeException("Unknown JsValue type " + jsval);
        }
        return value;
    }

    /**
     * Convert a BrowserChannel Value into a JsValue.
     * 
     * @param ccl Compiling class loader, may be null if val is known to not be a Java object or exception.
     * @param localObjects table of Java objects, may be null as above.
     * @param val Value to convert
     * @param jsval JsValue object to receive converted value.
     */
    public static void convertToJsValue(CompilingClassLoader ccl, ServerObjectsTable localObjects, Value val, JsValueOOPHM jsval) {
        switch (val.getType()) {
            case NULL:
                jsval.setNull();
                break;
            case BOOLEAN:
                jsval.setBoolean(val.getBoolean());
                break;
            case BYTE:
                jsval.setByte(val.getByte());
                break;
            case CHAR:
                jsval.setChar(val.getChar());
                break;
            case DOUBLE:
                jsval.setDouble(val.getDouble());
                break;
            case FLOAT:
                jsval.setDouble(val.getFloat());
                break;
            case INT:
                jsval.setInt(val.getInt());
                break;
            case LONG:
                jsval.setDouble(val.getLong());
                break;
            case SHORT:
                jsval.setShort(val.getShort());
                break;
            case STRING:
                jsval.setString(val.getString());
                break;
            case UNDEFINED:
                jsval.setUndefined();
                break;
            case JS_OBJECT:
                jsval.setJavascriptObject(val.getJsObject());
                break;
            case JAVA_OBJECT:
                assert ccl != null && localObjects != null;
                jsval.setWrappedJavaObject(ccl, localObjects.get(val.getJavaObject().getRefid()));
                break;
        }
    }

}
