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

import com.atlassian.clover.ant.tasks.CloverLogTask;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
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
    private void logDatabase(final String database) {
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

    /**
     * Configures test source roots for clover log task. It takes original test directory,
     * directories from maven compilation and directories from all submodules (aggregation).
     * @param cloverLogTask
     */
    private void setTestSourceRoots(final CloverLogTask cloverLogTask) {
        // take for current project
        setTestSourceRootsForProject(cloverLogTask, getProject());

        // do the same but for sub-modules
        for (MavenProject mavenProject : getDescendantModuleProjects(getProject())) {
            setTestSourceRootsForProject(cloverLogTask, mavenProject);
        }
    }

    /**
     * Configures test source roots for clover log task for a single maven project.
     * It takes original test directory and  directories from maven compilation.
     * @param cloverLogTask
     * @param project
     */
    private void setTestSourceRootsForProject(final CloverLogTask cloverLogTask, final MavenProject project) {
        // original src/test directory
        String originalSrcTestDir = CloverSetupMojo.getOriginalSrcTestDir(project.getId());
        if (originalSrcTestDir != null) {
            addTestSrcDir(cloverLogTask, originalSrcTestDir);
        }

        // src/test directories from maven compilation
        final List<String> testSourceRoots = project.getTestCompileSourceRoots();
        addTestSrcDirs(cloverLogTask, testSourceRoots.iterator());
    }

    /**
     * Adds a list of test source directories as an Ant's fileset to clover log task.
     * @param cloverLogTask
     * @param iterator Iterator<String>
     * @see CloverLogTask#addTestSources(org.apache.tools.ant.types.FileSet)
     */
    private void addTestSrcDirs(final CloverLogTask cloverLogTask, final Iterator<String> iterator) {
        while (iterator.hasNext()) {
            final String testDir = iterator.next();
            addTestSrcDir(cloverLogTask, testDir);
        }
    }

    /**
     * Adds new test source directory as an Ant's fileset to clover log task.
     * @param cloverLogTask
     * @param originalSrcTestDir
     * @see CloverLogTask#addTestSources(org.apache.tools.ant.types.FileSet)
     */
    private void addTestSrcDir(final CloverLogTask cloverLogTask, final String originalSrcTestDir) {
        final File dir = new File(originalSrcTestDir);
        if (dir.exists()) {
            final FileSet testFiles = new FileSet();
            testFiles.setDir(dir);
            cloverLogTask.addTestSources(testFiles);
        }
    }
}
