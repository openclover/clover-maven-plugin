package com.atlassian.maven.plugin.clover.it.e;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: tomd
 * Date: Sep 12, 2007
 * Time: 3:34:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestClassBE extends TestCase {
    public void testMethod()
    {
        assertEquals(42, new ClassBE().method());
    }
}
