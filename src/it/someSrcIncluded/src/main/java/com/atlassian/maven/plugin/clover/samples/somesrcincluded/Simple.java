/*
 * Copyright 2006 The Apache Software Foundation.
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
package com.atlassian.maven.plugin.clover.samples.somesrcincluded;

public class Simple
{
    public void someMethod1()
    {        
    }

    public void someMethodToReduceStatementCoverage()
    {
        try
        {
            System.getProperties();
        }
        finally
        {
            // We use a try/finally so that we can use a block context of 
        }
    }
    
    public void someMethod2(int i)
    {                
        if (i == 2)
        {
            //  Do nothing
        }
    }
} 