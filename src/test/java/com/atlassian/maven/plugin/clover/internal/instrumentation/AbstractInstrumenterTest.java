package com.atlassian.maven.plugin.clover.internal.instrumentation;

import com.atlassian.clover.cfg.instr.MethodContextDef;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.cmdline.CloverInstrArgProcessors;
import com.atlassian.maven.plugin.clover.MethodWithMetricsContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

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

}