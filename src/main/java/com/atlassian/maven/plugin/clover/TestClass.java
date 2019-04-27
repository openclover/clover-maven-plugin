package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.configuration.AbstractJavaEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches a single test class. Example:
 * <pre>
 * &lt;testClass&gt;
 *     &lt;name&gt;.*Test&lt;/name&gt;
 *     &lt;super&gt;WebTest&lt;/super&gt;
 *     &lt;annotation&gt;@MyTestFramework&lt;/annotation&gt;
 *     &lt;package&gt;org\.openclover\..*&lt;/package&gt;
 *     &lt;tag&gt;@returns&lt;/tag&gt;
 *     &lt;testMethods&gt;
 *         &lt;testMethod&gt;&lt;!-- see TestMethod --&gt;&lt;/testMethod&gt; &lt;!-- 0..N occurrences --&gt;
 *     &lt;/testMethods&gt;
 * &lt;/testClass&gt;
 * </pre>
 */
public class TestClass extends AbstractJavaEntity {

    /** A regex on which to match the test class's superclass. Optional. */
    private String superclass;

    /** A regex on which to match the test class's annotation. Optional. */
    private String packageName;

    private List<TestMethod> testMethods = new ArrayList<TestMethod>();

    @SuppressWarnings("unused") // called by Maven when parsing MOJO configuration
    public TestClass() {
    }

    public String getSuper() {
        return superclass;
    }

    public void setSuper(String superclass) {
        this.superclass = superclass;
    }

    public String getPackage() {
        return packageName;
    }

    public void setPackage(String packageName) {
        this.packageName = packageName;
    }

    public List<TestMethod> getTestMethods() {
        return testMethods;
    }

    public void setTestMethods(List<TestMethod> testMethods) {
        this.testMethods = testMethods;
    }

}
