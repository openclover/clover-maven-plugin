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
 * Computes the list of main source files to instrument.
 */
public class MainSourceScanner extends AbstractSourceScanner {
    @NotNull
    public static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java";

    @NotNull
    public static final String SRC_MAIN_GROOVY = "src" + File.separator + "main" + File.separator + "groovy";

    public MainSourceScanner(final CompilerConfiguration configuration, final String outputSourceDirectory) {
        super(configuration, outputSourceDirectory);
    }

    /**
     * From a list of provided <code>sourceRoots</code> remove SRC_MAIN_GROOVY root
     *
     * @param sourceRoots    list of source roots from
     * @see #SRC_MAIN_GROOVY
     * @see #getSourceFilesToInstrument()
     */
    @Override
    public void removeGroovySourceRoot(@NotNull final Set<String> sourceRoots) {
        removeSourceRoot(sourceRoots, SRC_MAIN_GROOVY);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSourceRootForLanguage(String sourceRoot, Language language) {
        return (language.getName().equals(Language.Builtin.JAVA.getName()) && sourceRoot.endsWith(SRC_MAIN_JAVA))
                || (language.getName().equals(Language.Builtin.GROOVY.getName()) && sourceRoot.endsWith(SRC_MAIN_GROOVY));
    }

    protected List<String> getCompileSourceRoots() {
        final List<String> roots = new ArrayList<String>(getConfiguration().getProject().getCompileSourceRoots());
        // add hardcoded SRC_MAIN_GROOVY (clover:setup might be called before the groovy-eclipse-plugin adds it
        // as a compilation root)
        roots.add(SRC_MAIN_GROOVY);
        return roots;
    }

    protected String getSourceDirectory() {
        return getConfiguration().getProject().getBuild().getSourceDirectory();
    }
}
