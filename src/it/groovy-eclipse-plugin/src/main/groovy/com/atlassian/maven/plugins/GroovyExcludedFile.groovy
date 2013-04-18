package com.atlassian.maven.plugins

/**
 * Test: file shall not be instrumented by Clover, but still compiled
 */
class GroovyExcludedFile {
    def excluded() {
        println 'GroovyExcludedFile.excluded: this shall not be in the report'
    }
}
