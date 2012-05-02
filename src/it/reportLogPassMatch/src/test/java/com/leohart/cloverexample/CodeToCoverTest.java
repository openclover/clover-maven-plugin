package com.leohart.cloverexample;

import org.junit.Assert;
import org.junit.Test;

public class CodeToCoverTest {

	@Test
	public void coverSomeCode() {
		CodeToCover codeToCover = new CodeToCover();

		Assert.assertTrue("Should have returned true: ",
				codeToCover.someMethodCoveredByTests());
	}
}
