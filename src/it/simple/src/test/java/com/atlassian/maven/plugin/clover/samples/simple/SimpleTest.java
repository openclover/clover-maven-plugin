/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atlassian.maven.plugin.clover.samples.simple;

import junit.framework.TestCase;
import java.util.Properties;

public class SimpleTest extends TestCase
{
    public void testSomeMethod() throws Exception
    {
        Simple simple = new Simple();
        simple.someMethod();
        
        // Verify that we can load resource files from tess when using the clover plugin
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/com/atlassian/maven/plugin/clover/samples/simple/test.properties"));
    }
}
 