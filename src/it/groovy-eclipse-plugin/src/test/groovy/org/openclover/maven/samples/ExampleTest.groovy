package org.openclover.maven.samples

import groovy.test.GroovyTestCase
/**
 * Tests for the {@link GroovyExample} class.
 */
class ExampleTest extends GroovyTestCase {
    void testShow() {
        new GroovyExample().show()
        new JavaExample().show()
    }
}
