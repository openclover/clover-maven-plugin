package com.atlassian.maven.plugin.clover.it.d;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: tomd
 * Date: Sep 12, 2007
 * Time: 3:16:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestClassBCD extends TestCase {
    public void testMethod()
    {
        assertEquals(2,new ClassBCD().method());
    }
}
