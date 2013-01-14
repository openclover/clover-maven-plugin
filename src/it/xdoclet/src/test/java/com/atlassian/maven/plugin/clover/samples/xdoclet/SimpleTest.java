/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atlassian.maven.plugin.clover.samples.xdoclet;

import junit.framework.TestCase;

import java.io.File;

public class SimpleTest extends TestCase
{
    public void testSomeMethod() throws Exception
    {
        final Simple simple = new Simple();
        simple.someMethod();

        final String buildDir = System.getProperty("project.build.directory");
        if (buildDir != null) {
            System.out.println("Build directory=" + buildDir);
            // Create a file in:
            //  target/test.clover        - default lifecycle
            //  target/clover/test.clover - forked lifecycle
            // See also verifications.xml
            final File cloverTestFile = new File(buildDir, "test.clover");
            cloverTestFile.createNewFile();
            assertTrue(cloverTestFile.isFile());
        } else {
            fail("The project.build.directory is not found!");
        }
    }
}
 