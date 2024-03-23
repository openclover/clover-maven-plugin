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

import org.junit.Test;

import java.util.concurrent.Callable;

public class SimpleTest {

    @Test
    public void testSomeMethod() {
        Simple simple = new Simple();
        simple.someMethod1();
        simple.someMethod2(1);
    }

    public int simpleMatch() {
        // 'simple' matches this as we have only 1 statement
        return 1;
    }

    public int simpleDoNotMatch() {
        // 'simple' does not match as we have more than 1 statement
        int i = 1;
        return i;
    }

    public int aggregatedMatch() {
        // 'aggregated' matches this as we have only 1 statement
        return 1;
    }

    public int aggregatedDoNotMatch() throws Exception {
        // 'aggregated' does not match this, although we have only 1 statement, there is a nested object having extra code
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                // this is a second statement
                return 1;
            }
        }.call();
    }

    public int anyMatch() {
        // 'any' matches this
        return 0;
    }

    public int anyMatchToo() {
        // 'any' matches this too as there's no limit on complexity
        int i = 0;
        i++;
        i++;
        i++;
        i++;
        i++;
        i++;
        i++;
        i++;
        return 0;
    }

}
