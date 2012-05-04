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

import com.cenqua.clover.tasks.CloverLogTask;
import com.cenqua.clover.tasks.CloverPassTask;
import org.apache.maven.plugin.MojoExecutionException;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Provides information on the current Clover database.
 *
 * @goal log
 * @phase post-integration-test
 */
public class CloverLogMojo extends AbstractCloverMojo {
    /**
     * Comma or space separated list of Clover contexts (block, statement or method filers) to exclude before
     * performing the check.
     *
     * @parameter expression="${maven.clover.contextFilters}"
     */
    String contextFilters;

    public void execute()
        throws MojoExecutionException {
        if (skip) {
            getLog().debug("Skipping clover log.");
            return;
        }

        if (areCloverDatabasesAvailable()) {
            super.execute();

            AbstractCloverMojo.waitForFlush(getWaitForFlush(), getFlushInterval());

            log();
        } else {
            getLog().info("No Clover database found, skipping Clover database logging");
        }
    }

    /**
     * Log information for both the main Clover database and the merged Clover database when they exist.
     */
    private void log() {
        if (new File(resolveCloverDatabase()).exists()) {
            logDatabase(resolveCloverDatabase());
        }
        if (new File(getCloverMergeDatabase()).exists()) {
            logDatabase(getCloverMergeDatabase());
        }
    }

    /**
     * Log information from a Clover database.
     *
     * @param database the Clover database to log
     */
    private void logDatabase(String database) {
        final Project antProject = new Project();
        antProject.init();
        AbstractCloverMojo.registerCloverAntTasks(antProject, getLog());

        CloverLogTask cloverLogTask = (CloverLogTask) antProject.createTask("clover-log");
        cloverLogTask.init();
        cloverLogTask.setInitString(database);
        cloverLogTask.setOutputProperty("cloverlogproperty");
        if (this.contextFilters != null) {
            cloverLogTask.setFilter(this.contextFilters);
        }
        setTestSourceRoots(cloverLogTask);
        cloverLogTask.execute();

        getLog().info(antProject.getProperty("cloverlogproperty"));
    }

    private void setTestSourceRoots(CloverLogTask cloverPassTask) {
        String originalSrcTestDir = CloverSetupMojo.getOriginalSrcTestDir(getProject().getArtifactId());
        if (originalSrcTestDir != null) {
            addTestSrcDir(cloverPassTask, originalSrcTestDir);
        }
        final List testSourceRoots = getProject().getTestCompileSourceRoots();
        for (Iterator iterator = testSourceRoots.iterator(); iterator.hasNext(); ) {
            String testDir = (String) iterator.next();
            addTestSrcDir(cloverPassTask, testDir);
        }
    }

    private void addTestSrcDir(CloverLogTask cloverLogTask, String originalSrcTestDir) {
        final FileSet testFiles = new FileSet();
        final File dir = new File(originalSrcTestDir);
        if (dir.exists()) {
            testFiles.setDir(dir);
            cloverLogTask.addTestSources(testFiles);
        }
    }
}
