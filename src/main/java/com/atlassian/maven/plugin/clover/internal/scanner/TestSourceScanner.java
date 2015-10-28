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

import com.atlassian.clover.spi.lang.Language;
import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Computes the list of test source files to instrument.
 */
public class TestSourceScanner extends AbstractSourceScanner {

    @NotNull
    public static final String SRC_TEST_JAVA = "src" + File.separator + "test" + File.separator + "java";

    @NotNull
    public static final String SRC_TEST_GROOVY = "src" + File.separator + "test" + File.separator + "groovy";

    public TestSourceScanner(final CompilerConfiguration configuration, final String outputSourceDirectory) {
        super(configuration, outputSourceDirectory);
    }

    /**
     * From a list of provided <code>sourceRoots</code> remove SRC_TEST_GROOVY root
     *
     * @param sourceRoots list of source roots
     * @see #SRC_TEST_GROOVY
     * @see #getSourceFilesToInstrument()
     */
    @Override
    public void removeGroovySourceRoot(@NotNull final Set<String> sourceRoots) {
        removeSourceRoot(sourceRoots, SRC_TEST_GROOVY);
    }

    /**
     * {@inheritDoc}
     */
    protected List<String> getCompileSourceRoots() {
        // take all compilation roots as defined in POM or added by other maven plugins
        final List<String> roots = new ArrayList<String>(getConfiguration().getProject().getTestCompileSourceRoots());
        // add hardcoded SRC_TEST_GROOVY (clover:setup might be called before the groovy-eclipse-plugin adds it
        // as a compilation root)
        roots.add(SRC_TEST_GROOVY);
        return roots;
    }

    protected String getSourceDirectory() {
        return getConfiguration().getProject().getBuild().getTestSourceDirectory();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSourceRootForLanguage(@NotNull String sourceRoot, @NotNull Language language) {
        return (language.getName().equals(Language.Builtin.JAVA.getName()) && sourceRoot.endsWith(SRC_TEST_JAVA))
                || (language.getName().equals(Language.Builtin.GROOVY.getName()) && sourceRoot.endsWith(SRC_TEST_GROOVY));
    }
}