package org.openclover.maven.samples
/**
 * Test: file shall not be instrumented by Clover, but still compiled and exectued
 */
class GroovyExcludedFileTest extends GroovyTestCase {
    void testExcluded() {
        println 'Executing GroovyExcludedFileTest.testExcluded'
        new GroovyExcludedFile().excluded()
    }
}
