package com.atlassian.maven.plugin.clover.it.r;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: tomd
 * Date: Sep 12, 2007
 * Time: 3:37:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestClassQPR extends TestCase {
    public void testMethod()
    {
        assertEquals(101, new ClassQPR().method());
    }
}
