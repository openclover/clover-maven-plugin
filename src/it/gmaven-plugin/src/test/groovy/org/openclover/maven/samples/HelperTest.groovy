package org.openclover.maven.samples

import groovy.util.GroovyTestCase

/**
 * Tests for the {@link Helper} class.
 */
class HelperTest extends GroovyTestCase {
    void testHelp() {
        new Helper().help(new Example())
    }
}
