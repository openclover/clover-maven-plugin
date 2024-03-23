package org.openclover.maven.samples
/**
 * Tests for the {@link GroovyExample} class.
 */
class ExampleTest extends GroovyTestCase {
    void testShow() {
        new GroovyExample().show()
        new JavaExample().show()
    }
}
