package com.atlassian.maven.plugin.clover.samples.testsources.excluded;

import com.atlassian.maven.plugin.clover.samples.testsources.TestSuite;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Does not match despite being named *One and having @TestSuite annotation, because file name matches the 'excludes'
 * pattern.
 */
@TestSuite
public class ExcludedOne {

    @Test
    public void testSomeMethod() {
        assertTrue(true);
    }
}
