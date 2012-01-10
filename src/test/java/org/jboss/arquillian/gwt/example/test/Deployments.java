package org.jboss.arquillian.gwt.example.test;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class Deployments {
    private static final String sample = "./sample.war";

    public static WebArchive sample() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File(sample));
    }
}
