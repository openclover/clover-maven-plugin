package com.atlassian.maven.plugins;

/**
 * Example Java class. It cannot be instrumented by Clover if placed in src/main/groovy.
 * See CloverInstrumentInternalMojo for details why.
 */
public class NotInstrumented {
    public void hello() {
        System.out.println("NotInstrumented.hello");
    }
}