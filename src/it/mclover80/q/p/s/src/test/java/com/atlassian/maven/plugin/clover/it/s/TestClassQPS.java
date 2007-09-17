package com.atlassian.maven.plugin.clover.it.s;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: tomd
 * Date: Sep 12, 2007
 * Time: 3:39:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestClassQPS extends TestCase {
    public void testMethod()
    {
        assertEquals(99,new ClassQPS().method());
    }
}
