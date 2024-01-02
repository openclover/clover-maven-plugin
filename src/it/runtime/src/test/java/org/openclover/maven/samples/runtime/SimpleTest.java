package org.openclover.maven.samples.runtime;

import org.junit.Test;

import java.util.Properties;

public class SimpleTest {
    @Test
    public void testSomeMethod() throws Exception {
        Simple simple = new Simple();
        simple.someMethod();

        // Verify that we can load resource files from tess when using the clover plugin
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/org/openclover/maven/samples/runtime/test.properties"));
    }
}
 