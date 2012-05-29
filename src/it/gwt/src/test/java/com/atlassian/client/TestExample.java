package com.atlassian.client;

import com.googlecode.gwt.test.GwtCreateHandler;
import com.googlecode.gwt.test.GwtTest;
import com.googlecode.gwt.test.rpc.RemoteServiceCreateHandler;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.*;
import java.util.Enumeration;


/**
 * Example test using the gwt-test-utils framework, which allows to test GWT applications
 * inside JVM, i.e. without a web browser. This allows to test client and shared code.
 *
 * Typically, you will have to prepare stubs for server-side services. You can make it
 * "manually" as in example below or use mocking frameworks like Mockito or EasyMock.
 * Please refer to
 *    <li>http://code.google.com/p/gwt-test-utils/wiki/MockingClasses</li>
 *    <li>http://code.google.com/p/gwt-test-utils/wiki/MockingRpcServices</li>
 *
 * We will test our main application class GwtExample with a simple stub for GreetingService.
 *
 * Class is named according to junit convention (Test*) in order to be automatically found
 * by maven-surefire-plugin in the <b><i>test</i></b> phase.
 */
public class TestExample extends GwtTest {

    /** Our application under test */
    private GwtExample app;

    /** A stub simulating behaviour of the server service */
    public static class GreetingServiceStub implements GreetingService {

        @Override
        public String greetServer(String name) throws IllegalArgumentException {
            if ( (name == null) || (name.length() < 4) ) {
                throw new IllegalArgumentException("Name must be at least 4 characters long");
            }

            return "Hello, " + name + "!<br><br>I am running [...]" +
                    ".<br><br>It looks like you are using:<br> [...]";
        }
    }

    /**
     * Return the fully qualified name of your GWT module
     */
    @Override
    public String getModuleName() {

        return "com.atlassian.GwtExample";
    }

    @Before
    public void init() throws Exception {
        // Create a handler which will intercept a GWT.create() call like:
        //    GreetingServiceAsync greetingService = GWT.create(GreetingService.class);
        GwtCreateHandler greetingServiceHandler = new RemoteServiceCreateHandler() {
            @Override
            protected Object findService(Class<?> remoteServiceClass, String remoteServiceRelativePath) {
                if (remoteServiceClass == GreetingService.class) {
                    return new GreetingServiceStub();
                }
                return null;
            }
        };
        addGwtCreateHandler(greetingServiceHandler);

        // create an instance of our application under test
        app = new GwtExample();
        app.onModuleLoad();

        // create fake servlet config
        setServletConfig(new ServletConfig() {
            @Override
            public String getServletName() {
                return "greetServlet";
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public String getInitParameter(String s) {
                return null;
            }

            @Override
            public Enumeration getInitParameterNames() {
                return null;
            }
        });
    }

    /**
     * This test will simulate sending a request to the server by filling the input field and clicking send button.
     * A GreetingServiceStub is used instead of a real service.
     */
    @Test
    public void testClickSuccess() {
        // fill form and send
        final String myName = "Alice";
        app.nameField.setText(myName);
        app.sendButton.click();

        // check results
        Assert.assertTrue("Substring [" + myName + "] was not found in [" + app.serverResponseLabel.getText() +"]",
                app.serverResponseLabel.getText().contains(myName));
    }
}
