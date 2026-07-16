package org.openclover.maven.samples
/**
 * GroovyExample Java class.
 */
class GroovyHelper {
    void help(final GroovyExample example) {
        println 'Calling GroovyExample from GroovyHelper'
        example.show()
    }

    void help(final JavaExample example) {
        println 'Calling JavaExample from GroovyHelper'
        example.show()
    }
}    