package com.atlassian.maven.plugin.clover;

import java.util.ArrayList;
import java.util.List;

/**
 * A container for test files, works like Ant's file set.
 * Example:
 * <pre>
 * &lt;testSources&gt;
 *     &lt;includes&gt;
 *         &lt;include&gt;**&#47;*Test.java&lt;/include&gt;
 *         &lt;include&gt;**&#47;*IT.java&lt;/include&gt;
 *     &lt;includes&gt;
 *     &lt;excludes&gt;
 *         &lt;exclude&gt;deprecated/**&lt;/exclude&gt;
 *     &lt;/excludes&gt;
 *     &lt;testClasses&gt;
 *         &lt;testClass&gt;&lt;!-- see TestClass --&gt;&lt;/testClass&gt; &lt;!-- 0..N occurrences --&gt;
 *     &lt;/testClasses&gt;
 * &lt;/testSources&gt;
 * </pre>
 */
public class TestSources {

    private final List<String> includes = new ArrayList<>();

    private final List<String> excludes = new ArrayList<>();

    private final List<TestClass> testClasses = new ArrayList<>();

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
