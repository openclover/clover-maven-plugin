package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.configuration.AbstractJavaEntity;

/**
 * Matches a single test method. Example:
 *
 * <pre>
 * &lt;testMethod&gt;
 *     &lt;name&gt;check.*&lt;/name&gt;
 *     &lt;annotation&gt;@Test&lt;/annotation&gt;
 *     &lt;tag&gt;@web&lt;/tag&gt;
 *     &lt;returntype&gt;void&lt;/returntype&gt;
 * &lt;/testMethod&gt;
 * </pre>
 */
public class TestMethod extends AbstractJavaEntity {

    /**
     * A regex on which to match the return type of the method, e.g.:
     * <ul>
     * <li><code>.*</code> - will match any return type.</li>
     * <li><code>void</code> - will match methods with no return type.</li>
     * </ul>
     */
    private String returnType;

    @SuppressWarnings("unused") // called by Maven when parsing MOJO configuration
    public TestMethod() {
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
}
