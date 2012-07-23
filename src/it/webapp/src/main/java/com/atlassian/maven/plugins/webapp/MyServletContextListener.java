package com.atlassian.maven.plugins.webapp;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class MyServletContextListener implements ServletContextListener {


    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.out.println("Web App Initialized");
    }

    ///CLOVER:OFF
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("Web App Destroyed");
    }
    ///CLOVER:ON
}
