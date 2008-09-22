package com.atlassian.maven.plugin.clover.samples.modulea;

import junit.framework.TestCase;

/**
 */
public class SlowTest extends TestCase {

    public void testWaitFor3Seconds() throws InterruptedException {
        Thread.sleep(3000);
        // just trying to get this to recompile
    }

}
