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

import com.atlassian.maven.plugin.clover.internal.scanner.CloverSourceScanner;
import com.atlassian.maven.plugin.clover.internal.scanner.TestSourceScanner;
import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;

import java.util.List;

/**
 * Instruments test sources.
 */
public class TestInstrumenter extends AbstractInstrumenter {
    private final TestSourceScanner scanner;

    public TestInstrumenter(final CompilerConfiguration configuration, final String outputSourceDirectory) {
        super(configuration, outputSourceDirectory);
        scanner = new TestSourceScanner(configuration, outputSourceDirectory);
    }

    protected CloverSourceScanner getSourceScanner() {
        return scanner;
    }

    protected String getSourceDirectory() {
        return getConfiguration().getProject().getBuild().getTestSourceDirectory();
    }

    protected void setSourceDirectory(final String targetDirectory) {
        getConfiguration().getProject().getBuild().setTestSourceDirectory(targetDirectory);
    }

    protected List<String> getCompileSourceRoots() {
        return getConfiguration().getProject().getTestCompileSourceRoots();
    }

    protected void addCompileSourceRoot(final String sourceRoot) {
        getConfiguration().getProject().addTestCompileSourceRoot(sourceRoot);
    }
}