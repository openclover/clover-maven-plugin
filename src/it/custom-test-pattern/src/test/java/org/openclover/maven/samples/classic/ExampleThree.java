package org.openclover.maven.samples.classic;

import static org.junit.Assert.assertTrue;

/**
 * Matches because it's in the 'testsources.classic' package AND has '@test' tag AND
 * file name matches the 'includes' pattern.
 *
 * @test
 */
public class ExampleThree {
    /**
     * This is a test.
     */
    public void testExampleThree() {
        assertTrue(true);
    }

    /**
     * This is not a test as it does not return 'void'
     */
    public int testNotExampleThree() {
        return -1;
    }
}
