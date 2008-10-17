package com.atlassian.maven.plugin.clover.samples.modulea;

import junit.framework.TestCase;

/**
 */
public class AlwaysRunTest extends TestCase {

    public void testMeAlways() {
        System.out.println("This is a test case that should always run, regardless of Clover's optimization.");
        assertTrue(true);
    }

}
