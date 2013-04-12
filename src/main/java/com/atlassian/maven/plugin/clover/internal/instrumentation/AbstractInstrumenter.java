package com.atlassian.maven.plugin.clover.internal.instrumentation;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.atlassian.maven.plugin.clover.MvnLogger;
import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;
import com.atlassian.maven.plugin.clover.internal.scanner.CloverSourceScanner;
import com.cenqua.clover.CloverInstr;
import com.cenqua.clover.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Code common for instrumentation of various source roots (main sources, test sources).
 */
public abstract class AbstractInstrumenter {
    private CompilerConfiguration configuration;

    String outputSourceDirectory;
    private static final String PROP_PROJECT_BUILD_SOURCEENCODING = "project.build.sourceEncoding";

    public AbstractInstrumenter(final CompilerConfiguration configuration, final String outputSourceDirectory) {
        this.configuration = configuration;
        this.outputSourceDirectory = outputSourceDirectory;
    }

    protected CompilerConfiguration getConfiguration() {
        return this.configuration;
    }

    public void instrument() throws MojoExecutionException {
        final CloverSourceScanner scanner = getSourceScanner();
        final Map<String, String[]> filesToInstrument = scanner.getSourceFilesToInstrument();
        if (filesToInstrument.isEmpty()) {
            getConfiguration().getLog().info("No Clover instrumentation done on source files in: "
                    + getCompileSourceRoots() + " as no matching sources files found");

        } else {
            instrumentSources(filesToInstrument, outputSourceDirectory);

        }

        // We need to copy excluded files as otherwise they won't be in the new Clover source directory and
        // thus won't be compiled by the compile plugin. This will lead to compilation errors if any other
        // Java file depends on any of these excluded files.

        if (configuration.copyExcludedFiles()) {
            copyExcludedFiles(scanner.getExcludedFiles(), outputSourceDirectory);
        }

        //won't do its job when include files set is empty!
    }

    public String redirectSourceDirectories() {
        return redirectSourceDirectories(outputSourceDirectory);
    }

    protected abstract CloverSourceScanner getSourceScanner();

    protected abstract String getSourceDirectory();

    protected abstract void setSourceDirectory(final String targetDirectory);

    protected abstract List<String> getCompileSourceRoots();

    protected abstract void addCompileSourceRoot(final String sourceRoot);

    private String redirectSourceDirectories(final String targetDirectory) {
        final String oldSourceDirectory = getSourceDirectory();
        if (new File(oldSourceDirectory).exists()) {
            setSourceDirectory(targetDirectory);
        }

        getConfiguration().getLog().debug("Clover source directories before change:");
        logSourceDirectories();

        // Maven2 limitation: changing the source directory doesn't change the compile source roots
        // See http://jira.codehaus.org/browse/MNG-1945
        List<String> sourceRoots = new ArrayList<String>(getCompileSourceRoots());

        // Clean all source roots to add them again in order to keep the same original order of source roots.
        getCompileSourceRoots().removeAll(sourceRoots);

        for (final String sourceRoot : sourceRoots) {
            if (new File(oldSourceDirectory).exists() && sourceRoot.equals(oldSourceDirectory)) {
                addCompileSourceRoot(getSourceDirectory());
            }
            // ignore the generated sources directory, because a new clover/generated-sources directory has been created by a plugin
            // during the clover forked lifecycle, which means that we will end up with the same classes included twice
            // if we use the generated-classes directory added originally
            else if (!isGeneratedSourcesDirectory(sourceRoot) && !getConfiguration().includesAllSourceRoots()) {
                addCompileSourceRoot(sourceRoot);
            }
        }

        getConfiguration().getLog().debug("Clover main source directories after change:");
        logSourceDirectories();
        return oldSourceDirectory;
    }

    private boolean isGeneratedSourcesDirectory(final String sourceRoot) {
        String generatedSourcesDirectoryName = File.separator + "target" + File.separator + "generated-sources";
        return sourceRoot.indexOf(generatedSourcesDirectoryName) != -1;
    }

    private void logSourceDirectories() {
        if (getConfiguration().getLog().isDebugEnabled()) {
            for (String sourceRoot : getCompileSourceRoots()) {
                getConfiguration().getLog().debug("[Clover]  source root [" + sourceRoot + "]");
            }
        }
    }

    /**
     * Copy all files that have been excluded by the user (using the excludes configuration property). This is required
     * as otherwise the excluded files won't be in the new Clover source directory and thus won't be compiled by the
     * compile plugin. This will lead to compilation errors if any other Java file depends on any of them.
     *
     * @throws MojoExecutionException if a failure happens during the copy
     */
    private void copyExcludedFiles(final Map<String, String[]> excludedFiles, final String targetDirectory) throws MojoExecutionException {
        for (String sourceRoot : excludedFiles.keySet()) {
            final String[] filesInSourceRoot = excludedFiles.get(sourceRoot);

            for (String fileName : filesInSourceRoot) {
                final File srcFile = new File(sourceRoot, fileName);
                try {
                    configuration.getLog().debug("Copying excluded file: " + srcFile.getAbsolutePath() + " to " + targetDirectory);
                    FileUtils.copyFile(srcFile, new File(targetDirectory,
                            srcFile.getPath().substring(sourceRoot.length())));
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to copy excluded file [" + srcFile + "] to ["
                            + targetDirectory + "]", e);
                }
            }
        }
    }

    private void instrumentSources(final Map<String, String[]> filesToInstrument, final String outputDir) throws MojoExecutionException {

        Logger.setInstance(new MvnLogger(configuration.getLog()));
        // only make dirs when there is src to instrument. see CLMVN-118
        new File(outputDir).mkdirs();
        int result = CloverInstr.mainImpl(createCliArgs(filesToInstrument, outputDir));
        if (result != 0) {
            throw new MojoExecutionException("Clover has failed to instrument the source files "
                    + "in the [" + outputDir + "] directory");
        }
    }

    /**
     * @return the CLI args to be passed to CloverInstr
     */
    private String[] createCliArgs(final Map<String, String[]> filesToInstrument, final String outputDir) throws MojoExecutionException {
        final List<String> parameters = new ArrayList<String>();

        parameters.add("-p");
        parameters.add(getConfiguration().getFlushPolicy());
        parameters.add("-f");
        parameters.add("" + getConfiguration().getFlushInterval());

        parameters.add("-i");
        parameters.add(getConfiguration().resolveCloverDatabase());

        parameters.add("-d");
        parameters.add(outputDir);

        if (getConfiguration().getLog().isDebugEnabled()) {
            parameters.add("-v");
        }

        if (getConfiguration().getDistributedCoverage() != null && getConfiguration().getDistributedCoverage().isEnabled()) {
            parameters.add("--distributedCoverage");
            parameters.add(getConfiguration().getDistributedCoverage().toString());
        }

        if (getConfiguration().getJdk() != null) {
            if (getConfiguration().getJdk().equals("1.4")) {
                parameters.add("--source");
                parameters.add("1.4");
            } else if (getConfiguration().getJdk().equals("1.5")) {
                parameters.add("--source");
                parameters.add("1.5");
            } else if (getConfiguration().getJdk().equals("1.6")) {
                parameters.add("--source");
                parameters.add("1.6");
            } else if (getConfiguration().getJdk().equals("1.7")) {
                parameters.add("--source");
                parameters.add("1.7");
            } else {
                throw new MojoExecutionException("Unsupported jdk version [" + getConfiguration().getJdk()
                        + "]. Valid values are [1.4], [1.5], [1.6] and [1.7]");
            }
        }

        if (!getConfiguration().isUseFullyQualifiedJavaLang()) {
            parameters.add("--dontFullyQualifyJavaLang");
        }

        if (getConfiguration().getEncoding() != null) {
            parameters.add("--encoding");
            parameters.add(getConfiguration().getEncoding());
        } else if (getConfiguration().getProject().getProperties().get(PROP_PROJECT_BUILD_SOURCEENCODING) != null) {
            parameters.add("--encoding");
            parameters.add(getConfiguration().getProject().getProperties().get(PROP_PROJECT_BUILD_SOURCEENCODING).toString());
        }

        if (getConfiguration().getInstrumentation() != null) {
            parameters.add("--instrlevel");
            parameters.add(getConfiguration().getInstrumentation());
        }

        for (final String srcDir : filesToInstrument.keySet()) {
            final String[] filesInSourceRoot = filesToInstrument.get(srcDir);
            for (String s : filesInSourceRoot) {
                File file = new File(srcDir, s);
                parameters.add(file.getPath());
            }
        }

        // custom contexts
        addCustomContexts(parameters, getConfiguration().getMethodContexts().entrySet(), "-mc");
        addCustomContexts(parameters, getConfiguration().getStatementContexts().entrySet(), "-sc");

        // Log parameters
        if (getConfiguration().getLog().isDebugEnabled()) {
            getConfiguration().getLog().debug("Parameter list being passed to Clover CLI:");
            for (String param : parameters) {
                getConfiguration().getLog().debug("  parameter = [" + param + "]");
            }
        }

        return parameters.toArray(new String[0]);
    }

    private void addCustomContexts(final List<String> parameters, final Set<Map.Entry<String, String>> contexts, final String flag) {
        for (final Map.Entry<String, String> entry : contexts) {
            parameters.add(flag);
            parameters.add(entry.getKey() + "=" + entry.getValue());
        }
    }
}

