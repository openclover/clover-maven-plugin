package com.atlassian.maven.plugin.clover.samples.moduleb;

import junit.framework.TestCase;
import com.atlassian.maven.plugin.clover.samples.modulea.Simple;
import com.atlassian.maven.plugin.clover.samples.modulea.Dummy;

/**
 * This test, tests the class in module-a
 */
public class CrossModuleTest extends TestCase {

    public void testModuleA() {
        Simple simple = new Simple();
        simple.someMethod();
        assertNotNull(simple);
    }

    public void testModuleAAgain() {
        Dummy simple = new Dummy();
        simple.dummyMethod();
        assertNotNull(simple);
    }
}
