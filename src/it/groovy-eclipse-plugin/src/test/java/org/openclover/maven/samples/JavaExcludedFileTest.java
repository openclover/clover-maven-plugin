package org.openclover.maven.samples;

import junit.framework.TestCase;

/**
 * Tests for the {@link GroovyHelper} class.
 */
public class JavaExcludedFileTest extends TestCase {

    public void testExcluded() {
        System.out.println("Executing JavaExcludedFileTest.testExcluded");
        new JavaExcludedFile().excluded();
    }

}
