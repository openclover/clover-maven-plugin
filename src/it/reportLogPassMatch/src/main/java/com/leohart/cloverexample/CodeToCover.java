package com.leohart.cloverexample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class CodeToCover {

	private static final Logger LOG = LoggerFactory.getLogger(CodeToCover.class);

	private String someProperty;

	public CodeToCover() {
		//This should be excluded by the "Constructor" context
	}

	public CodeToCover(final String someProperty) {
		//This should be excluded by the "Constructor" context
		super();
		this.someProperty = someProperty;
	}

	public String getSomeProperty() {
		//This should be excluded by the "Property" context
		return this.someProperty;
	}

	public void setSomeProperty(final String someProperty) {
		//This should be excluded by the "Property" context
		this.someProperty = someProperty;
	}

	public Boolean someMethodCoveredByTests() {
		System.out.println("Covered!");

		if (CodeToCover.LOG.isInfoEnabled()) {
			CodeToCover.LOG
					.info("I should be excluded via a custom statement context.");
		}

		return true;
	}

	public Boolean someMethodExcludedViaCustomMethodContext() {
		System.out.println("Excluded!");

		return true;
	}

	public Boolean someMethodNotCoveredByTests() {
		System.out.println("Should not be covered!");

		return true;
	}

}
