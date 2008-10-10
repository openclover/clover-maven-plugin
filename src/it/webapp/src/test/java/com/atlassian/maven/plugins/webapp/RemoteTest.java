package com.atlassian.maven.plugins.webapp;

import junit.framework.TestCase;
import net.sourceforge.jwebunit.junit.WebTester;

/**
 */
public class RemoteTest extends TestCase {


    private WebTester tester;

    public RemoteTest(String name) {
        super(name);
        tester = new WebTester();
    }

    public void testRemoteServlet() {
        final String url = System.getProperty("webapp.url") + ":" + System.getProperty("http.port") + System.getProperty("webapp.context");
        System.out.println("TESTING REMOTE URL = " + url);
        tester.getTestContext().setBaseUrl(url);
        tester.beginAt("/remote");
        tester.assertMatch("Clover");
    }
    

}
