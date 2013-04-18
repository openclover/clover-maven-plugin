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

import java.util.Map;

/**
 * Scans source roots and return list of files to instrument.
 */
public interface CloverSourceScanner {
    /**
     * Returns the list of source files to instrument taking into account the includes and excludes specified by
     * the user. The Map is indexed on source roots.
     *
     * @return Map&lt;String, String[]&gt; = Map(source root, files)
     */
    Map<String, String[]> getSourceFilesToInstrument();

    /**
     * Returns the list of source files to instrument taking into account the includes and excludes specified by
     * the user and additionaly taking into account a programming language. The Map is indexed on source roots.
     *
     *
     * @param languageFileFilter extra filter (in addition to includes/excludes) based on programming language
     * @param skipGroovySourceDirectory if <code>true</code> then don't list source files which are located in the
     *                                  source directory 'native' for groovy language
     * @return Map&lt;String, String[]&gt; = Map(source root, files)
     */
    Map<String, String[]> getSourceFilesToInstrument(LanguageFileFilter languageFileFilter, boolean skipGroovySourceDirectory);

    /**
     * Returns the list of excluded files that we'll need to copy. This is required as otherwise the excluded files
     * won't be in the new Clover source directory and thus won't be compiled by the compile plugin. This will
     * lead to compilation errors if any other Java file depends on any of them. The Map is indexed on
     * source roots.
     *
     * @return Map&lt;String, String[]&gt; = Map(source root, files)
     */
    Map<String, String[]> getExcludedFiles();

    /*
     * Returns true if given <code>sourceRoot</code> is a native source root for given language. For example:
     * <pre>
     *   isSourceRootForLanguage("src/main/java", Language.Builtin.JAVA) = true
     *   isSourceRootForLanguage("src/test/groovy", Language.Builtin.GROOVY) = true
     *   isSourceRootForLanguage("src/test/scala", Language.Builtin.GROOVY) = false
     * </pre>
     *
     * @param sourceRoot directory to be checked
     * @param language   programming language
     */
    boolean isSourceRootForLanguage(String sourceRoot, Language language);
}
