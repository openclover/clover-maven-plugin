package org.openclover.maven.samples

import groovy.test.GroovyTestCase

class GroovyHelperTest extends GroovyTestCase {
    void testGroovyExampleHelp() {
        new GroovyHelper().help(new GroovyExample())
    }

    void testJavaExampleHelp() {
        new GroovyHelper().help(new JavaExample())
    }
}
