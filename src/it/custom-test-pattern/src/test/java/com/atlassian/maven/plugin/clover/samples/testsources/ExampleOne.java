package com.atlassian.maven.plugin.clover.samples.testsources;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Matches because class is named '*One' AND has @TestSuite annotation AND file name matches 'includes' pattern.
 */
@TestSuite
public class ExampleOne {

    /**
     * Matches because it has @Test annotation.
     */
    @Test
    public void testExampleOne() {
        assertTrue(true);
    }

    /**
     * Does not match because annotation is missing.
     */
    public int notTestExampleOne() {
        return 1;
    }

}

