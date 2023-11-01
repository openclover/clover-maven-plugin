package com.atlassian.maven.plugin.clover.internal.instrumentation;

import com.atlassian.clover.cfg.instr.MethodContextDef;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.cmdline.CloverInstrArgProcessors;
import com.atlassian.maven.plugin.clover.MethodWithMetricsContext;
import com.atlassian.maven.plugin.clover.TestClass;
import com.atlassian.maven.plugin.clover.TestMethod;
import com.atlassian.maven.plugin.clover.TestSources;
import org.junit.Test;
import org.openclover.util.Lists;
import org.openclover.util.Sets;

import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class AbstractInstrumenterTest {

    @Test
    public void testAddMethodWithMetricsContexts() {
        // test that MOJO's configuration is converted to proper arg line for CloverInstr
        final List<String> parameters = Lists.newArrayList("abc");
        final Set<MethodWithMetricsContext> contexts = Sets.newHashSet(
            new MethodWithMetricsContext("getter", "public .* get\\(\\)", 1, 2, 3, 4),
            new MethodWithMetricsContext("setter", "public void set\\(.*\\)", 4, 5, 6, 7)
        );
        AbstractInstrumenter.addMethodWithMetricsContexts(parameters, contexts);

        assertThat(parameters, hasItems(
                "abc",
                "-mmc",
                "getter;public .* get\\(\\);2;1;4;3",
                "-mmc",
                "setter;public void set\\(.*\\);5;4;7;6"));

        // double-check that it can be parsed by CloverInstr's argument parser
        String[] parametersArray = parameters.toArray(new String[0]);
        assertTrue(CloverInstrArgProcessors.MethodWithMetricsContext.matches(parametersArray, 1));

        JavaInstrumentationConfig config = new JavaInstrumentationConfig();
        CloverInstrArgProcessors.MethodWithMetricsContext.process(parametersArray, 1, config);
        assertThat(config.getMethodContexts().get(0), equalTo(
                new MethodContextDef("getter", "public .* get\\(\\)", 2, 1, 4, 3)
        ));
    }

    @Test
    public void testAddTestSources() {
        // test that MOJO's configuration is converted to proper arg line for CloverInstr
        final List<String> parameters = Lists.newArrayList("abc");

        final TestSources testSources = new TestSources();
        testSources.getIncludes().add("**/include/*One.java");
        testSources.getIncludes().add("**/include/two/**.java");
        testSources.getExcludes().add("**/include/exclude/**.java");
        testSources.getExcludes().add("**/deprecated/**.java");

        final TestMethod method11 = new TestMethod();
        method11.setName("test.*");
        method11.setReturnType("void");

        final TestMethod method12 = new TestMethod();
        method12.setAnnotation("Test");

        final TestClass testClass1 = new TestClass();
        testClass1.setPackage("com.acme.*");
        testClass1.setSuper("SuperClass");
        testClass1.setAnnotation("TestSuite");
        testClass1.setTestMethods(Lists.newArrayList(method11, method12));
        testSources.getTestClasses().add(testClass1);

        final TestMethod method21 = new TestMethod();
        method21.setTag("test");

        final TestClass testClass2 = new TestClass();
        testClass2.setName(".*IT");
        testClass2.setTag("test");
        testClass2.setTestMethods(Lists.newArrayList(method21));
        testSources.getTestClasses().add(testClass2);

        // convert Maven's mojo argument to list of command line options
        AbstractInstrumenter.addTestSources(parameters, testSources, "src/test/java");

        // check that we have args as expected
        assertThat(parameters, hasItems(
                "abc",
                "-tsr",
                "src/test/java",
                "-tsi",
                "**/include/*One.java,**/include/two/**.java",
                "-tse",
                "**/include/exclude/**.java,**/deprecated/**.java",
                "-tsc",
                ";com.acme.*;TestSuite;SuperClass;",
                "-tsm",
                "test.*;;void;",
                "-tsm",
                ";Test;;",
                "-tsc",
                ".*IT;;;;test",
                "-tsm",
                ";;;test"
        ));

        // double-check that it can be parsed by CloverInstr's argument parser
        // - argument prefix is recognized
        final String[] parametersArray = parameters.toArray(new String[0]);
        assertTrue(CloverInstrArgProcessors.TestSourceRoot.matches(parametersArray, 1));
        assertTrue(CloverInstrArgProcessors.TestSourceIncludes.matches(parametersArray, 3));
        assertTrue(CloverInstrArgProcessors.TestSourceExcludes.matches(parametersArray, 5));
        assertTrue(CloverInstrArgProcessors.TestSourceClass.matches(parametersArray, 7));
        assertTrue(CloverInstrArgProcessors.TestSourceMethod.matches(parametersArray, 9));
        assertTrue(CloverInstrArgProcessors.TestSourceMethod.matches(parametersArray, 11));
        assertTrue(CloverInstrArgProcessors.TestSourceClass.matches(parametersArray, 13));
        assertTrue(CloverInstrArgProcessors.TestSourceMethod.matches(parametersArray, 15));

        // - value for argument is parsed
        final JavaInstrumentationConfig config = new JavaInstrumentationConfig();
        CloverInstrArgProcessors.TestSourceRoot.process(parametersArray, 1, config);
        CloverInstrArgProcessors.TestSourceIncludes.process(parametersArray, 3, config);
        CloverInstrArgProcessors.TestSourceExcludes.process(parametersArray, 5, config);
        CloverInstrArgProcessors.TestSourceClass.process(parametersArray, 7, config);
        CloverInstrArgProcessors.TestSourceMethod.process(parametersArray, 9, config);
        CloverInstrArgProcessors.TestSourceMethod.process(parametersArray, 11, config);
        CloverInstrArgProcessors.TestSourceClass.process(parametersArray, 13, config);
        CloverInstrArgProcessors.TestSourceMethod.process(parametersArray, 15, config);
    }

}