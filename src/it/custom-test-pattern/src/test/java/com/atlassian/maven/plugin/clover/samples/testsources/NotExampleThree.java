package com.atlassian.maven.plugin.clover.samples.testsources;

import static org.junit.Assert.assertTrue;

/**
 * Does not match because it's not in the 'testsources.classes' package
 *
 * @test
 */
public class NotExampleThree {
    public void testNotExampleThree() {
        assertTrue(true);
    }
}
