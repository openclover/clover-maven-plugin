package com.atlassian.maven.plugin.clover;

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

import com.atlassian.maven.plugin.clover.internal.AbstractCloverInstrumentMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * <p>Fork a custom build lifecycle in which all sources will be instrumented by Clover and next execute this
 * lifecycle till the <code>install</code> phase. All instrumented classes will be stored in a separate directory. Similarly,
 * artifacts produced will have the 'clover' classifier.</p>
 * <p>This goal is forking a lifecycle because we don't want the Clover instrumentation to affect the main lifecycle
 * build. This will prevent instrumented sources to be put in production by error. Thus running
 * <code>mvn install</code> on a project where this <code>instrument</code> goal has been specified will run the
 * build twice: once for building the project as usual and another time for instrumenting the sources with Clover
 * and generating the Clover database.</p>
 *
 * <p><b>Attention: Maven does not support multiple classifiers for an artifact.</b>
 * In case your project creates artifacts with classifiers, it may happen that the 'clover' classifier will be lost and
 * an instrumented artifact will be installed as non-instrumented one. </p>
 *
 * <p>Example: clover:instrument + jar:test-jar + install:install</p>
 *
 * <pre>
 *     [INFO] --- maven-jar-plugin:2.6:test-jar (default) @ moneybags ---
 *     [INFO] Building jar: .../moneybags-1.0-SNAPSHOT-clover-tests.jar &lt;&lt;&lt; file with double classifier was created
 *     [INFO] --- maven-install-plugin:2.5.2:install (default-install) @ moneybags ---
 *     [INFO] Installing .../moneybags-1.0-SNAPSHOT-clover-tests.jar to
 *            ~/.m2/.../moneybags-1.0-SNAPSHOT-tests.jar &lt;&lt;&lt; but 'clover' classifier was lost
 * </pre>
 *
 * <p>In order to avoid this, you can use the <code>instrument-test</code> goal, which runs a forked lifecycle till
 * the <code>test</code> phase.</p>
 *
 * @goal instrument
 * @execute phase="install" lifecycle="clover"
 */
public class CloverInstrumentMojo extends AbstractCloverInstrumentMojo {

    /**
     * {@inheritDoc}
     *
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute() throws MojoExecutionException {
        super.execute();
    }

    @Override
    protected boolean shouldRedirectArtifacts() {
        return true;
    }

    @Override
    protected boolean shouldRedirectOutputDirectories() {
        return true;
    }
}
