package com.atlassian.maven.plugin.clover.samples.testsources.excluded;

import com.atlassian.maven.plugin.clover.samples.testsources.ExampleOne;
import com.atlassian.maven.plugin.clover.samples.testsources.ExampleTwoIT;
import com.atlassian.maven.plugin.clover.samples.testsources.NoTestSuiteSoNotOne;
import com.atlassian.maven.plugin.clover.samples.testsources.NotExampleThree;
import com.atlassian.maven.plugin.clover.samples.testsources.TestSuite;
import com.atlassian.maven.plugin.clover.samples.testsources.classic.ExampleThree;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Does not match despite being named *One and having @TestSuite annotation, because file name matches the 'excludes'
 * pattern.
 */
@TestSuite
public class ExcludedOne {

    @Before
    public void setUp() {
        // execute some code via surefire to generate coverage and see if a tests were recorded or not
        new ExampleOne().testExampleOne();
        new ExampleOne().notTestExampleOne();
        new ExampleTwoIT().getRequest();
        new ExampleTwoIT().postRequest();
        new ExampleTwoIT().deleteRequest();
        new NoTestSuiteSoNotOne().thisIsNotATestMethod();
        new NotExampleThree().testNotExampleThree();
        new ExampleThree().testExampleThree();
        new ExampleThree().testNotExampleThree();
    }

    @Test
    public void testSomeMethod() {
        assertTrue(true);
    }
}
