package com.atlassian.maven;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AppTest {


    @Test
    public void testAdd() {
        Arithmetic math = new Arithmetic();
        Assert.assertEquals(2, math.add(1, 1));
    }

    @Test
    public void testSubtract() {
        Arithmetic math = new Arithmetic();
        Assert.assertEquals(0, math.subtract(1, 1));

    }


}
