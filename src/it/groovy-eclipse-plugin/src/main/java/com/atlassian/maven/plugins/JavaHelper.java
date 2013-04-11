package com.atlassian.maven.plugins;

/**
 * GroovyExample Java class.
 */
public class JavaHelper {
    public void help(final GroovyExample example) {
        System.out.println("Calling GroovyExample from JavaHelper");
        example.show();
    }

    public void help(final JavaExample example) {
        System.out.println("Calling JavaExample from JavaHelper");
        example.show();
    }
}    