package com.atlassian.maven.plugin.clover.internal.scanner;

import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class GroovyTestScanner extends AbstractCloverSourceScanner {
    public GroovyTestScanner(final CompilerConfiguration configuration, final String outputDirectory) {
        super(configuration, outputDirectory);
    }

    protected List<String> getSourceRoots() {
        return new ArrayList(Arrays.asList(new String[]{"src/test/groovy"}));
    }

    protected String getSourceDirectory() {
        return new File(getConfiguration().getProject().getBuild().getTestSourceDirectory()).getParent();
    }
}
