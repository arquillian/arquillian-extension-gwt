Arquillian Google Web Toolkit Extension
===============

The Arquillian GWT extension brings true integration testing to GWT. This means that GWT integration tests 
can break free and execute in the actual runtime container instead of being tied to GWT’s embedded Jetty
server. Further it is possible to combine GWT client-side and standard in-container tests in the same test 
class. This allows to test features without having to worry about the client-server bridge and should pave 
the way for future support of Arquillian Warp and Drone in GWT integration tests.

Getting Started
-------

If you're new to Arquillian take a look a the [Getting Started Guide](http://arquillian.org/guides/getting_started/). 
To start using the Arquillian GWT extension just add the following dependency to your project.

    <dependency>
        <groupId>org.jboss.arquillian.extension</groupId>
        <artifactId>arquillian-gwt</artifactId>
        <version>1.0.0.Alpha1</version>
    </dependency>

Make sure this dependency is specified before all GWT dependencies (gwt-user, gwt-dev, gwt-servlet) in your pom.xml. 
The Arquillian GWT extension requires GWT 2.5.0 or higher.

Writing Arquillian GWT tests
------------------

To run a test method as a GWT integration test either annotate the test class or test method 
with `@RunAsGwtClient` and specifiy the GWT module. If you annotate the test class all test methods in 
that class will run as GWT integration tests.
```java
@RunWith(Arquillian.class)
public class MyTestClass {

  @Test 
  @RunAsGwtClient(moduleName = "org.myapp.MyGwtModule")
  public void myGwtTest() {
  
  }
}
```
Here's a complete example:

```java
@RunWith(Arquillian.class)
public class GreeterRpcTest extends ArquillianGwtTestCase {

  @Deployment
  public static WebArchive createDeployment() {
    return ShrinkWrap.create(WebArchive.class, "test.war")
      .addClass(Greeter.class)
      .addClass(GreetingService.class)
      .addClass(GreetingServiceImpl.class)
      .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"));
  }

  @Test
  @RunAsGwtClient(moduleName = "org.myapp.MyGwtModule")
  public void testGreetingService() {
    GreetingServiceAsync greetingService = GWT.create(GreetingService.class);
    greetingService.greetServer("Hello!", new AsyncCallback<String>() {
      @Override
      public void onFailure(Throwable caught) {
        Assert.fail("Request failure: " + caught.getMessage());
      }

      @Override
      public void onSuccess(String result) {
        assertEquals("Received invalid response from Server", "Welcome!", result);
        finishTest();
      }
    });
    delayTestFinish(5000);
  }
}
```
You will only need to extend `ArquillianGwtTestCase` if you want to inherit GWT's asynchronous testing 
methods (`finishTest` and `delayTestFinish`). 

Community/Help/Feedback
---------

* IRC: #jbosstesting @ irc.freenode.net
* [Forums](https://community.jboss.org/en/arquillian/dev)
* [Issue Tracker](https://issues.jboss.org/browse/ARQ/component/12316141)
