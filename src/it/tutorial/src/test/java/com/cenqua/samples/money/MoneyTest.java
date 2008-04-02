
package com.cenqua.samples.money;


public class MoneyTest extends SuperMoneyTest {

     public void testBagMultiply() {
		// {[12 CHF][7 USD]} *2 == {[24 CHF][14 USD]}
		IMoney expected= MoneyBag.create(new Money(24, "CHF"), new Money(14, "USD"));
		assertEquals(expected, fMB1.multiply(2));
		assertEquals(fMB1, fMB1.multiply(1));
		assertTrue(fMB1.multiply(0).isZero());
	}

}