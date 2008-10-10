package com.atlassian.maven.plugins.webapp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.util.Enumeration;

/**
 */
public class RemoteServlet extends HttpServlet {


    public void init(ServletConfig servletConfig) throws ServletException {
        Enumeration names = servletConfig.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            System.out.println(name + " = " + servletConfig.getInitParameter(name));
        }
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Hello, Server World! " + request.getServletPath());
        request.getRequestDispatcher("/my.jsp").forward(request, response);
    }


    private void noTestsComeHere() {
        String msg = "No tests reach me";
        System.out.println(msg);
        if (true) {
            System.out.println(msg);            
        }
    }
}
