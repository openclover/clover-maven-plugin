package com.atlassian.maven.plugin.clover;

import java.util.ArrayList;
import java.util.List;

/**
 * A container for test files, works like Ant's file set.
 * Example:
 * <pre>
 * &lt;testsources&gt;
 *     &lt;includes&gt;
 *         &lt;include&gt;**&#47;*Test.java&lt;/include&gt;
 *         &lt;include&gt;**&#47;*IT.java&lt;/include&gt;
 *     &lt;includes&gt;
 *     &lt;excludes&gt;
 *         &lt;exclude&gt;deprecated/**&lt;/exclude&gt;
 *     &lt;/excludes&gt;
 *     &lt;testclasses&gt;
 *         &lt;testclass&gt;&lt;!-- see TestClass --&gt;&lt;/testClass&gt; &lt;!-- 0..N occurrences --&gt;
 *     &lt;/testclasses&gt;
 * &lt;/testsources&gt;
 * </pre>
 */
public class TestSources {

    private List<String> includes = new ArrayList<String>();

    private List<String> excludes = new ArrayList<String>();

    private List<TestClass> testClasses = new ArrayList<TestClass>();

    @SuppressWarnings("unused") // called by Maven when parsing MOJO configuration
    public TestSources() {
    }

    public List<TestClass> getTestClasses() {
        return testClasses;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }
}
