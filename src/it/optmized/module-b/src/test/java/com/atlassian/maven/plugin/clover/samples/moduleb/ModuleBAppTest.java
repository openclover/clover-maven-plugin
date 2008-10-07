package com.atlassian.maven.plugin.clover.samples.moduleb;

import junit.framework.TestCase;

public class ModuleBAppTest extends TestCase {

    public void testGetName() {
        assertEquals(ModuleBApp.getApp().getName(), "ModuleBApp");
    }


    public void testGetNumber() {
        assertEquals(ModuleBApp.getApp().getNumber(), 0);
    }

}