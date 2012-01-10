package org.jboss.arquillian.gwt.example.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.gwt.RunAsGwtClient;
import org.jboss.arquillian.gwt.example.client.GreetingService;
import org.jboss.arquillian.gwt.example.client.GreetingServiceAsync;
import org.jboss.arquillian.gwt.example.shared.FieldVerifier;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.thoughtworks.selenium.DefaultSelenium;

@RunWith(Arquillian.class)
@RunAsGwtClient(moduleName = "org.jboss.arquillian.gwt.example.sampleJUnit")
public class GWTUnitTest {

    @Deployment(testable = false)
    public static WebArchive sample() {
        return Deployments.sample();
    }

    @Drone
    DefaultSelenium selenium;

    /**
     * Tests the FieldVerifier.
     */
    @Test
    public void testFieldVerifier() {
        Assert.assertFalse("Null is invalid name", FieldVerifier.isValidName(null));
        Assert.assertFalse("Empty name is invalid name", FieldVerifier.isValidName(""));
        Assert.assertFalse("One char is invalid length", FieldVerifier.isValidName("a"));
        Assert.assertFalse("Two chars is invalid length", FieldVerifier.isValidName("ab"));
        Assert.assertFalse("Three chars is invalid length", FieldVerifier.isValidName("abc"));
        Assert.assertTrue("Four chars is valid length", FieldVerifier.isValidName("abcd"));
    }

    /**
     * This test will send a request to the server using the greetServer method in GreetingService and verify the response.
     */
    @Test
    public void testGreetingService() {
        // Create the service that we will test.
        GreetingServiceAsync greetingService = GWT.create(GreetingService.class);
        ServiceDefTarget target = (ServiceDefTarget) greetingService;
        target.setServiceEntryPoint(GWT.getModuleBaseURL() + "sample/greet");

        System.out.println("EntryPoint" + GWT.getModuleBaseURL() + "sample/greet");

        // Since RPC calls are asynchronous, we will need to wait for a response
        // after this test method returns. This line tells the test runner to wait
        // up to 10 seconds before timing out.
        // delayTestFinish(10000);

        // Send a request to the server.
        greetingService.greetServer("GWT User", new AsyncCallback<String>() {
            public void onFailure(Throwable caught) {
                // The request resulted in an unexpected error.
                Assert.fail("Request failure: " + caught.getMessage());
            }

            public void onSuccess(String result) {
                // Verify that the response is correct.
                Assert.assertTrue("Response starts with Hello, GWT User!", result.startsWith("Hello, GWT User!"));

                // Now that we have received a response, we need to tell the test runner
                // that the test is complete. You must call finishTest() after an
                // asynchronous test finishes successfully, or the test will time out.
                // finishTest();
            }
        });
    }

}
