package com.atlassian.maven.plugins

import groovy.util.GroovyTestCase

/**
 * Tests for the {@link GroovyHelper} class.
 */
class GroovyHelperTest extends GroovyTestCase {
    void testGroovyExampleHelp() {
        new GroovyHelper().help(new GroovyExample())
    }

    void testJavaExampleHelp() {
        new GroovyHelper().help(new JavaExample())
    }
}
