package com.atlassian.maven;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AppTest {
    @Test
    public void testAdd() {
        Arithmetic math = new Arithmetic();
        Assert.assertEquals(math.add(1, 1), 2);
    }

    @Test
    public void testSubtract() {
        Arithmetic math = new Arithmetic();
        Assert.assertEquals(math.subtract(1, 1), 0);
    }
}
