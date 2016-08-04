/*
 * Copyright 2016 Atlassian.
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
package com.atlassian.maven.plugin.clover.samples.somesrcincluded;

import com.atlassian.maven.plugin.clover.samples.somesrcincluded.include.Simple2;
import com.atlassian.maven.plugin.clover.samples.somesrcincluded.include.Simple3;
import junit.framework.TestCase;

public class SimpleTest extends TestCase
{
    public void testSomeMethod()
    {        
        Simple simple = new Simple();
        Simple2 simple2 = new Simple2();
        Simple3 simple3 = new Simple3();
        simple.someMethod1();
        simple.someMethod2(1);
        simple2.someMethod2(2);
        simple3.someMethod3(3);
    }
} 