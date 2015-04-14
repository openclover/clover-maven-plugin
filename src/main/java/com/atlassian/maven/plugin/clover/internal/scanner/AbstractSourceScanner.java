package com.atlassian.maven.plugin.clover.internal.scanner;

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

import com.google.common.collect.Iterables;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.DependSelector;
import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Code common to compute the list of source files to instrument (main sources, test sources) for Java and Groovy
 * languages.
 */
public abstract class AbstractSourceScanner implements CloverSourceScanner {

    private interface SourceRootVisitor {
        void visitDir(File dir);
    }

    @NotNull
    private final CompilerConfiguration configuration;

    @NotNull
    private final File targetDir;

    /**
     *
     * @param configuration compiler configuration
     * @param outputSourceDirectory where to put instrumented sources
     */
    public AbstractSourceScanner(@NotNull final CompilerConfiguration configuration, @NotNull final String outputSourceDirectory) {
        this.configuration = configuration;
        this.targetDir = new File(outputSourceDirectory);
    }

    /**
     * {@inheritDoc}
     *
     * This method handles a special case: don't return excludes from native Groovy source directory (src/main/groovy
     * or src/test/groovy) because such files shall not be copied to instrumented sources directory
     * (target/clover/src-instrumented or target/clover/src-test/instrumented); a reason is that gmaven-plugin and
     * groovy-eclipse-plugin have the src/xxx/groovy location hardcoded and they will compile this source root
     * no matter what other compilation source roots or source directory are provided; it means that we would end
     * up with a 'duplicate class' build error if files are copied.
     *
     * @see CloverSourceScanner#getExcludedFiles()
     */
    public Map<String,String[]> getExcludedFiles() {
        Map<String, String[]> excludedFiles = computeExcludedFiles(getDirectoryScanner());
        // special case: don't return excludes from 'src/(main|test)/groovy'
        removeGroovySourceRoot(excludedFiles.keySet());
        return excludedFiles;
    }

    /**
     * {@inheritDoc}
     *
     * @see CloverSourceScanner#getSourceFilesToInstrument()
     */
    public Map<String,String[]> getSourceFilesToInstrument() {
        return getSourceFilesToInstrument(LanguageFileExtensionFilter.ANY_LANGUAGE, false);
    }

    /**
     *
     * This method can handle a special case: don't return excludes from native Groovy source directory (src/main/groovy
     * or src/test/groovy) because such files shall not be copied to instrumented sources directory
     * (target/clover/src-instrumented or target/clover/src-test/instrumented); a reason is that gmaven and
     * groovy-eclipse-plugin have the src/xxx/groovy location hardcoded and they will compile this source root
     * no matter what other compilation source roots or source directory are provided; it means that we would end
     * up with a 'duplicate class' build error if files would be copied.
     *
     * Please note however than in case when Groovy sources are located in a native Java source directory (src/main/java
     * or src/test/java) then such files must be copied. A reason is that source roots will be redirected to the
     * Clover's instrumented directory (target/clover/src-instrumented or target/clover/src-test/instrumented) and the
     * groovy compiler must find both Java and Groovy files.
     *
     * @param languageFileFilter extra filter (in addition to includes/excludes) based on the programming language
     * @param skipGroovySourceDirectory if <code>true</code> then it will not return files located under Groovy
     *                                  source directory (i.e. 'src/main/groovy' or 'src/test/groovy')
     * @return Map&lt;File,String[]&gt;
     */
    public Map<String,String[]> getSourceFilesToInstrument(LanguageFileFilter languageFileFilter, boolean skipGroovySourceDirectory) {
        Map<String, String[]> includedFiles = computeIncludedFiles(getDirectoryScanner(), languageFileFilter);
        // special case: don't return includes from 'src/(main|test)/groovy'
        if (skipGroovySourceDirectory) {
            removeGroovySourceRoot(includedFiles.keySet());
        }
        return includedFiles;
    }

    protected abstract List<String> getCompileSourceRoots();

    protected abstract String getSourceDirectory();

    protected CompilerConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * From a list of provided <code>sourceRoots</code> remove those which specific for this scanner,
     * unless the specific folder points to the same location as the {@link #getSourceDirectory()}.
     *
     * @param sourceRoots list to be modified
     * @see #getSourceFilesToInstrument()
     */
    protected abstract void removeGroovySourceRoot(@NotNull final Set<String> sourceRoots);

    protected void removeSourceRoot(final Set<String> sourceRoots, String sourceRootToRemove) {
        for (final Iterator<String> iter = sourceRoots.iterator(); iter.hasNext(); ) {
            final String sourceRoot = iter.next();
            if (sourceRoot.endsWith(sourceRootToRemove)) {
                iter.remove();
            }
        }
    }

    /**
     * Returns a Plexus scanner object that scans a source root and filters files according to inclusion and
     * exclusion patterns. In our case at hand we include only Java sources as these are the only files we want
     * to instrument.
     * @return DirectoryScanner
     */
    private DirectoryScanner getDirectoryScanner() {
        final Set<String> includes = getConfiguration().getIncludes();
        final Set<String> excludes = getConfiguration().getExcludes();

        configuration.getLog().debug("excludes patterns = " + excludes);
        configuration.getLog().debug("includes patterns = " + includes);
        final DirectoryScanner dirScan = new DirectoryScanner();

        dirScan.addExcludes(Iterables.toArray(excludes, String.class));
        dirScan.setIncludes(Iterables.toArray(includes, String.class));

        dirScan.addDefaultExcludes();

        final DependSelector selector = new DependSelector();
        selector.setTargetdir(targetDir);
        dirScan.setSelectors(new FileSelector[]{selector});

        return dirScan;
    }

    private Map<String, String[]> computeExcludedFiles(final DirectoryScanner scanner) {
        final Map<String, String[]> files = new HashMap<String,String[]>();

        visitSourceRoots(new SourceRootVisitor() {
            public void visitDir(File dir) {
                scanner.setBasedir(dir);

                final Set<String> configurationIncludes = getConfiguration().getIncludes();
                final String[] includes = concatArrays(
                        Iterables.toArray(configurationIncludes, String.class),
                        DirectoryScanner.getDefaultExcludes());
                scanner.setIncludes(includes);// ensure that .svn dirs etc are not considered excluded
                scanner.scan();

                final String[] sourcesToAdd = concatArrays(scanner.getExcludedFiles(), scanner.getNotIncludedFiles());

                configuration.getLog().debug("excluding files from instrumentation = " + Arrays.asList(sourcesToAdd));

                if (sourcesToAdd.length > 0) {
                    files.put(dir.getPath(), sourcesToAdd);
                }
            }
        });
        return files;
    }

    private Map<String, String[]> computeIncludedFiles(final DirectoryScanner scanner, final LanguageFileFilter languageFilter) {
        final Map<String, String[]> files = new HashMap<String,String[]>();
        visitSourceRoots(new SourceRootVisitor() {
            public void visitDir(File dir) {
                scanner.setBasedir(dir);
                scanner.scan();
                final String[] sourcesToAdd = languageFilter.filter(scanner.getIncludedFiles());
                if (sourcesToAdd.length > 0) {
                    configuration.getLog().debug("including files for instrumentation = " + Arrays.asList(sourcesToAdd));
                    files.put(dir.getAbsolutePath(), sourcesToAdd);
                }
            }
        });

        return files;
    }

    private String[] concatArrays(final String[] a1, final String[] a2) {
        final String[] result = new String[a1.length + a2.length];
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }

    private boolean isGeneratedSourcesDirectory(final String sourceRoot) {
        final String generatedSrcDirDefaultLifecycle = File.separator + "target" + File.separator + "generated-sources";
        final String generatedSrcDirCloverLifecycle = File.separator + "target" + File.separator + "clover" + File.separator + "generated-sources";
        return sourceRoot.indexOf(generatedSrcDirDefaultLifecycle) != -1
                || sourceRoot.indexOf(generatedSrcDirCloverLifecycle) != -1;
    }

    /**
     * Returns list of source roots in which source files shall be searched. If configuration has
     * <code>includesAllSourceRoots=true</code> then it will return generated sources as well.
     * @return List&lt;String&gt;
     */
    private List<String> getResolvedSourceRoots() {
        final List<String> sourceRoots = new ArrayList<String>();
        if (getConfiguration().isIncludesAllSourceRoots()) {
            // take all roots
            sourceRoots.addAll(getCompileSourceRoots());
        } else {
            // take non-generated source roots
            for (String sourceRoot : getCompileSourceRoots()) {
                if (!isGeneratedSourcesDirectory(sourceRoot)) {
                    sourceRoots.add(sourceRoot);
                }
            }
        }
        return sourceRoots;
    }


    private void visitSourceRoots(final SourceRootVisitor visitor) {
        // Decide whether to instrument all source roots or only the main source root.
        for (String resolvedSourceRoot : getResolvedSourceRoots()) {
            final File sourceRoot = new File(resolvedSourceRoot);
            if (sourceRoot.exists()) {
                visitor.visitDir(sourceRoot);
            }
        }
    }

}
