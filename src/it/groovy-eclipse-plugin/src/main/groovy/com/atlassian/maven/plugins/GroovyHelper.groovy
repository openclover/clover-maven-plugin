package com.atlassian.maven.plugins;

/**
 * GroovyExample Java class.
 */
public class GroovyHelper {
    public void help(final GroovyExample example) {
        println 'Calling GroovyExample from GroovyHelper'
        example.show();
    }

    public void help(final JavaExample example) {
        println 'Calling JavaExample from GroovyHelper'
        example.show();
    }
}    