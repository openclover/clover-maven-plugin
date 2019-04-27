package com.atlassian.maven.plugin.clover;

import org.apache.tools.ant.types.FileSet;

import java.util.ArrayList;
import java.util.List;

/**
 * A container for test files, works like Ant's file set.
 * Example:
 * <pre>
 * &lt;testsources&gt;
 *     &lt;includes&gt;**&#47;*Test.java,**&#47;*IT.java&lt;includes&gt;
 *     &lt;excludes&gt;deprecated/**&lt;/excludes&gt;
 *     &lt;testclasses&gt;
 *         &lt;testclass&gt;&lt;!-- see TestClass --&gt;&lt;/testClass&gt; &lt;!-- 0..N occurrences --&gt;
 *     &lt;/testclasses&gt;
 * &lt;/testsources&gt;
 * </pre>
 */
public class TestSources extends FileSet {
    private List<TestClass> testClasses = new ArrayList<TestClass>();

    @SuppressWarnings("unused") // called by Maven when parsing MOJO configuration
    public TestSources() {
    }

    public List<TestClass> getTestClasses() {
        return testClasses;
    }
}
