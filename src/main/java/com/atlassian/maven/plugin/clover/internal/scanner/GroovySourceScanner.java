package com.atlassian.maven.plugin.clover.internal.scanner;

import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class GroovySourceScanner extends AbstractCloverSourceScanner {

    public GroovySourceScanner(final CompilerConfiguration configuration, final String outputSourceDirectory) {
        super(configuration, outputSourceDirectory);
    }

    protected List<String> getSourceRoots() {
        return new ArrayList(Arrays.asList(new String[]{"src/main/groovy"}));
    }

    protected String getSourceDirectory() {
        return new File(getConfiguration().getProject().getBuild().getSourceDirectory()).getParent();
    }
}
