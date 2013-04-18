package com.atlassian.maven.plugins;

import junit.framework.TestCase;

/**
 * Tests for the {@link com.atlassian.maven.plugins.GroovyHelper} class.
 */
public class JavaExcludedFileTest extends TestCase {

    public void testExcluded() {
        System.out.println("Executing JavaExcludedFileTest.testExcluded");
        new JavaExcludedFile().excluded();
    }

}
