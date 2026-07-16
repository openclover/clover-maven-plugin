package org.openclover.maven.samples;

import static org.junit.Assert.assertTrue;

/**
 * Matches because it inherits from 'WebTest' class AND file name matches 'includes' pattern.
 */
public class ExampleTwoIT extends WebTest {

    /**
     * Matches 'get' prefix.
     */
    public void getRequest() {
        assertTrue(true);
    }

    /**
     * Matches 'post' prefix.
     */
    public void postRequest() {
        assertTrue(true);
    }

    /**
     * Not a test because method name does not have get/post prefix
     */
    public void deleteRequest() {
        assertTrue(true);
    }

}
