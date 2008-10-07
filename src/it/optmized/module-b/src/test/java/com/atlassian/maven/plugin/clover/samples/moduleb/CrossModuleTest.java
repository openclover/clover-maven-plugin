package com.atlassian.maven.plugin.clover.samples.moduleb;

import junit.framework.TestCase;
import com.atlassian.maven.plugin.clover.samples.modulea.Simple;

/**
 * This test, tests the class in module-a
 */
public class CrossModuleTest extends TestCase {

    public void testModuleA() {
        Simple simple = new Simple();
        assertNotNull(simple);
    }
}
