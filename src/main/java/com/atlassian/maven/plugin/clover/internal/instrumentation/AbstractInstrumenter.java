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

import com.atlassian.clover.CloverInstr;
import com.atlassian.clover.Logger;
import com.atlassian.clover.spi.lang.Language;
import com.atlassian.maven.plugin.clover.MvnLogger;
import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;
import com.atlassian.maven.plugin.clover.internal.scanner.CloverSourceScanner;
import com.atlassian.maven.plugin.clover.internal.scanner.LanguageFileExtensionFilter;
import com.google.common.collect.Iterables;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    /**
     *
     * @throws MojoExecutionException when instrumentation fails
     * @see com.atlassian.maven.plugin.clover.CloverInstrumentInternalMojo#calcIncludedFilesForGroovy()
     * @see com.atlassian.maven.plugin.clover.CloverInstrumentInternalMojo#redirectOutputDirectories()
     */
    public void instrument() throws MojoExecutionException {
        final CloverSourceScanner scanner = getSourceScanner();
        // get source files to be instrumented, but only for Java as they will be instrumented by CloverInstr
        final Map<String, String[]> javaFilesToInstrument = scanner.getSourceFilesToInstrument(LanguageFileExtensionFilter.JAVA_LANGUAGE, true);
        if (javaFilesToInstrument.isEmpty()) {
            getConfiguration().getLog().info("No Clover instrumentation done on source files in: "
                    + getCompileSourceRoots() + " as no matching sources files found (JAVA_LANGUAGE)");
        } else {
            instrumentSources(javaFilesToInstrument, outputSourceDirectory);
        }

        // find groovy files in all compilation roots and copy them
        //
        // 1) in case when 'src/main/java' (or 'src/test/java') contains *.groovy source files (this is a trick possible
        // with a groovy-eclipse-plugin, see http://groovy.codehaus.org/Groovy-Eclipse+compiler+plugin+for+Maven
        // "Setting up source folders / Do nothing") we must copy *.groovy files as well
        // reason: 'src/main/java' (or 'src/test/java') will be redirected to 'target/clover/src-instrumented'
        // (or 'target/clover/src-test-instrumented') and Groovy compiler must be able to find these groovy sources
        //
        // 2) however we shall not copy groovy files from 'src/(main|test)/groovy' because these source roots are not
        // being redirected to 'target/clover/src-(test-)instrumented'; furthermore groovy-eclipse-plugin has
        // 'src/(main|test)/groovy' location hardcoded, so copying files would end up with 'duplicate class' build error
        final Map<String, String[]> groovyFilesToInstrument = scanner.getSourceFilesToInstrument(LanguageFileExtensionFilter.GROOVY_LANGUAGE, true);

        // copy groovy files
        if (!groovyFilesToInstrument.isEmpty()) {
            copyExcludedFiles(groovyFilesToInstrument, outputSourceDirectory);
        }

        // We need to copy excluded files too as otherwise they won't be in the new Clover source directory and
        // thus won't be compiled by the compile plugin. This will lead to compilation errors if any other
        // file depends on any of these excluded files.
        if (configuration.isCopyExcludedFiles()) {
            final Map<String, String[]> explicitlyExcludedFiles = scanner.getExcludedFiles();
            // 'src/(main|test)/groovy' is already filtered-out in getExcludedFiles()
            copyExcludedFiles(explicitlyExcludedFiles, outputSourceDirectory);
        }
    }

    public String redirectSourceDirectories() {
        return redirectSourceDirectories(outputSourceDirectory);
    }

    protected abstract CloverSourceScanner getSourceScanner();

    protected abstract String getSourceDirectory();

    protected abstract void setSourceDirectory(final String targetDirectory);

    protected abstract List<String> getCompileSourceRoots();

    protected abstract void addCompileSourceRoot(final String sourceRoot);

    protected abstract boolean isGeneratedSourcesDirectory(final String sourceRoot);

    private String redirectSourceDirectories(final String targetDirectory) {
        final String oldSourceDirectory = getSourceDirectory();
        if (new File(oldSourceDirectory).exists()) {
            setSourceDirectory(targetDirectory);
        }

        getConfiguration().getLog().debug("Clover " + getSourceType() + " source directories before change:");
        logSourceDirectories();

        // Maven2 limitation: changing the source directory doesn't change the compile source roots
        // See http://jira.codehaus.org/browse/MNG-1945
        final List<String> sourceRoots = new ArrayList<String>(getCompileSourceRoots());

        // Clean all source roots to add them again in order to keep the same original order of source roots.
        getCompileSourceRoots().removeAll(sourceRoots);

        final CloverSourceScanner scanner = getSourceScanner();
        for (final String sourceRoot : sourceRoots) {
            // if includeAllSourceRoots=true then all source roots will be redirected to the location of instrumented sources
            // if includeAllSourceRoots=false then we don't redirect generated source roots
            boolean needsRedirection = getConfiguration().isIncludesAllSourceRoots() ||
                    !isGeneratedSourcesDirectory(sourceRoot);

            // a) if it's a Java directory then use location of instrumented sources instead of the original source
            // root (e.g. 'src/main/java' -> 'target/clover/src-instrumented')
            // b) if it's a Groovy directory then don't change the location because we don't instrument Groovy on
            // a source level, so the Clover's instrumented folder is empty; Groovy files will be instrumented
            // during compilation on the AST level (e.g. 'src/main/groovy' -> 'src/main/groovy')
            if (scanner.isSourceRootForLanguage(sourceRoot, Language.Builtin.GROOVY))  {
                addCompileSourceRoot(sourceRoot);
            } else {
                addCompileSourceRoot(needsRedirection ? getSourceDirectory() : sourceRoot);
            }
        }

        getConfiguration().getLog().debug("Clover " + getSourceType() + " source directories after change:");
        logSourceDirectories();
        return oldSourceDirectory;
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

        final String javaLevel = getConfiguration().getJdk();
        if (javaLevel != null) {
            if (javaLevel.matches("1\\.[345678]")) {
                parameters.add("--source");
                parameters.add(javaLevel);
            } else {
                throw new MojoExecutionException("Unsupported java language level version [" + javaLevel
                        + "]. Valid values are [1.3], [1.4], [1.5], [1.6], [1.7] and [1.8]");
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

        if (getConfiguration().getInstrumentLambda() != null) {
            parameters.add("--instrlambda");
            parameters.add(getConfiguration().getInstrumentLambda());
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

        return Iterables.toArray(parameters, String.class);
    }

    private void addCustomContexts(final List<String> parameters, final Set<Map.Entry<String, String>> contexts, final String flag) {
        for (final Map.Entry<String, String> entry : contexts) {
            parameters.add(flag);
            parameters.add(entry.getKey() + "=" + entry.getValue());
        }
    }

    protected abstract String getSourceType();
}

