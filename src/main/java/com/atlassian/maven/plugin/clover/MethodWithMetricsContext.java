package com.atlassian.maven.plugin.clover;

import org.openclover.core.cfg.instr.MethodContextDef;

/**
 * Complex configuration objects need to be in the same package as the MOJO that defines them.
 */
public class MethodWithMetricsContext extends MethodContextDef {

    @SuppressWarnings("unsued") // called by Maven when parsing MOJO configuration
    public MethodWithMetricsContext() {
        super();
    }

    public MethodWithMetricsContext(String name, String regexp, int maxComplexity, int maxStatements, int maxAggregatedComplexity, int maxAggregatedStatements) {
        super(name, regexp, maxComplexity, maxStatements, maxAggregatedComplexity, maxAggregatedStatements);
    }
}
