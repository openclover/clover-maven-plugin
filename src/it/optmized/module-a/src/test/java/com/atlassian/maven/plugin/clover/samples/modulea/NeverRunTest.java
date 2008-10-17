package com.atlassian.maven.plugin.clover.samples.modulea;

import junit.framework.TestCase;

/**
 * A test which should never get run.
 */
public class NeverRunTest extends TestCase {

    public void testOfFAIL() {
        fail("This test should n'er get run. " +
                "It is here to ensure that excluding tests works correctly for optimized builds.");
    }
}
