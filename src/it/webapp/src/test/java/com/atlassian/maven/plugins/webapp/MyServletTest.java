package com.atlassian.maven.plugins.webapp;

import junit.framework.TestCase;
import net.sourceforge.jwebunit.junit.WebTester;

import javax.servlet.ServletException;

/**
 */
public class MyServletTest extends TestCase {


    private WebTester tester;

    public MyServletTest(String name) {
        super(name);
        tester = new WebTester();
    }

    public void testMyServlet() {
        final String url = System.getProperty("webapp.url") + ":" + System.getProperty("http.port") + System.getProperty("webapp.context");
        System.out.println("TESTING URL = " + url);
        tester.getTestContext().setBaseUrl(url);
        tester.beginAt("/my");
        tester.assertMatch("Clover");
    }

    public void testLocal() throws ServletException {
        MyServlet servlet = new MyServlet();
        servlet.init();
        assertNull(servlet.getServletConfig());
    }

}
