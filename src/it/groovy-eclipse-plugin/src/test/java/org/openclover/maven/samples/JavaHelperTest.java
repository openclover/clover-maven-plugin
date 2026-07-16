package org.openclover.maven.samples;

import junit.framework.TestCase;

/**
 * Tests for the {@link GroovyHelper} class.
 */
public class JavaHelperTest extends TestCase {

    public void testGroovyExampleHelp() {
        new JavaHelper().help(new GroovyExample());
    }

    public void testJavaExampleHelp() {
        new JavaHelper().help(new JavaExample());
    }
}
