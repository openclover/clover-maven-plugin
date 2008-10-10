package com.atlassian.maven.plugins.webapp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 */
public class MyServlet extends HttpServlet {

    public void init() throws ServletException {
        super.init();
        System.out.println(this + " STARTED.");
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Hello, New World = " + request.getHeaderNames());
        request.getRequestDispatcher("/my.jsp").forward(request, response);
    }
}
