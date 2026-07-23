package org.openclover.maven.samples

import groovy.test.GroovyTestCase

class ExampleTest extends GroovyTestCase {
    void testShow() {
        new GroovyExample().show()
        new JavaExample().show()
    }
}
