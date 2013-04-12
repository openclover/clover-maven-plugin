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

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.DependSelector;
import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 * Code common to compute the list of source files to instrument (main sources, test sources).
 */
public abstract class AbstractCloverSourceScanner implements CloverSourceScanner {
    private final CompilerConfiguration configuration;
    private final File targetDir;

    public AbstractCloverSourceScanner(final CompilerConfiguration configuration, final String outputSourceDirectory) {
        this.configuration = configuration;
        this.targetDir = new File(outputSourceDirectory);

    }

    protected CompilerConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * {@inheritDoc}
     *
     * @see CloverSourceScanner#getSourceFilesToInstrument()
     */
    public Map<String, String[]> getSourceFilesToInstrument() {
        return computeIncludedFiles(getScanner());
    }

    /**
     * {@inheritDoc}
     *
     * @see CloverSourceScanner#getExcludedFiles()
     */
    public Map<String, String[]> getExcludedFiles() {

        return computeExcludedFiles(getScanner());
    }

    protected abstract List<String> getSourceRoots();

    protected abstract String getSourceDirectory();

    /**
     * @return a Plexus scanner object that scans a source root and filters files according to inclusion and
     *         exclusion patterns. In our case at hand we include only Java sources as these are the only files we want
     *         to instrument.
     */
    private DirectoryScanner getScanner() {

        Set<String> includes = getConfiguration().getIncludes();
        Set<String> excludes = getConfiguration().getExcludes();

        configuration.getLog().debug("excludes patterns = " + excludes);
        configuration.getLog().debug("includes patterns = " + includes);
        DirectoryScanner dirScan = new DirectoryScanner();

        dirScan.addExcludes(excludes.toArray(new String[excludes.size()]));
        dirScan.setIncludes(includes.toArray(new String[includes.size()]));

        dirScan.addDefaultExcludes();

        DependSelector selector = new DependSelector();
        selector.setTargetdir(targetDir);
        dirScan.setSelectors(new FileSelector[]{selector});

        return dirScan;
    }

    private Map<String, String[]> computeExcludedFiles(final DirectoryScanner scanner) {
        final Map<String, String[]> files = new HashMap<String, String[]>();

        visitSourceRoots(new SourceRootVisitor() {
            public void visitDir(File dir) {
                scanner.setBasedir(dir);

                final String[] configuredIncludes = getConfiguration().getIncludes().toArray(new String[]{});
                final String[] includes = concatArrays(configuredIncludes, DirectoryScanner.getDefaultExcludes());
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

    private Map<String, String[]> computeIncludedFiles(final DirectoryScanner scanner) {
        final Map<String, String[]> files = new HashMap<String, String[]>();
        visitSourceRoots(new SourceRootVisitor() {
            public void visitDir(File dir) {
                scanner.setBasedir(dir);
                scanner.scan();
                final String[] sourcesToAdd = scanner.getIncludedFiles();
                configuration.getLog().debug("including files for instrumentation = " + Arrays.asList(sourcesToAdd));
                if (sourcesToAdd.length > 0) {
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

    private List<String> getResolvedSourceRoots() {

        return getConfiguration().includesAllSourceRoots() ?
                getSourceRoots() :
                Collections.singletonList(getSourceDirectory());
    }

    interface SourceRootVisitor {
        void visitDir(File dir);
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
