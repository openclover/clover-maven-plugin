package com.atlassian.maven.plugin.clover.internal;

import com.atlassian.maven.plugin.clover.DistributedCoverage;
import com.atlassian.maven.plugin.clover.MethodWithMetricsContext;
import com.atlassian.maven.plugin.clover.TestSources;

import java.util.Map;
import java.util.Set;

public interface CompilerConfiguration extends CloverConfiguration {

    Set<String> getIncludes();

    Set<String> getExcludes();

    boolean isIncludesAllSourceRoots();

    String getJdk();

    String getFlushPolicy();

    int getFlushInterval();

    boolean isUseFullyQualifiedJavaLang();

    String getEncoding();

    Map<String,String> getMethodContexts();

    Set<MethodWithMetricsContext> getMethodWithMetricsContexts();

    Map<String,String> getStatementContexts();

    DistributedCoverage getDistributedCoverage();

    int getStaleMillis();

    String getInstrumentation();

    String getInstrumentLambda();

    boolean isCopyExcludedFiles();

    TestSources getTestSources();

    boolean isRecordTestResults();
}
