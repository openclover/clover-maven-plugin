package com.atlassian.maven.plugin.clover.samples.testsources;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Does not match because it does not have @TestSuite annotation.
 */
public class NoTestSuiteSoNotOne {
    @Test
    public void thisIsNotATestMethod() {
        assertTrue(true);
    }
}
