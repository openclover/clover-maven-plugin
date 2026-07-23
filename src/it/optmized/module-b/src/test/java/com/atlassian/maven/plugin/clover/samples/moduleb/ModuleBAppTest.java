package com.atlassian.maven.plugin.clover.samples.moduleb;

import junit.framework.TestCase;

public class ModuleBAppTest extends TestCase {

    public void testGetName() {
        assertEquals("ModuleBApp", ModuleBApp.getApp().getName());
    }

    public void testGetNumber() {
        assertEquals(0, ModuleBApp.getApp().getNumber());
    }
}