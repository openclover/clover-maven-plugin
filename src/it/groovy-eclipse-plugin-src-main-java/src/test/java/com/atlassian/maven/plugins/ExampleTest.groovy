package com.atlassian.maven.plugins

import groovy.util.GroovyTestCase

/**
 * Tests for the {@link Example} class.
 */
class ExampleTest extends GroovyTestCase {
    void testShow() {
        new Example().show()
    }
    void testHelper() {
        new Helper()
    }
}
