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

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.atlassian.clover.CloverMerge;
import com.atlassian.clover.cfg.Interval;
import com.google.common.collect.Iterables;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate children module Clover databases if there are any. This mojo should not exist. It's only there because
 * the site plugin doesn't handle @aggregators properly at the moment...
 *
 * @goal aggregate
 * @aggregator
 */
public class CloverAggregateMojo extends AbstractCloverMojo {
    /**
     * Time span that will be used when generating aggregated database. Check
     * http://confluence.atlassian.com/display/CLOVER/Using+Spans and
     * http://confluence.atlassian.com/display/CLOVER/clover-merge.
     *
     * @parameter expression="${maven.clover.span}"
     */
    private String span = Interval.DEFAULT_SPAN.toString();

    /**
     * {@inheritDoc}
     *
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().debug("Skipping clover aggregate.");
            return;
        }

        // If we're in a module with children modules, then aggregate the children clover databases.
        if (getProject().getModules() != null && getProject().getModules().size() > 0) {
            super.execute();
            getLog().debug("Project " + getProject().getId() + " (" + getProject().getBasedir()
                    + ") has " + getProject().getModules().size() + " child modules");

            // Ensure all databases are flushed
            AbstractCloverMojo.waitForFlush(getWaitForFlush(), getFlushInterval());

            final List<String> childrenDatabases = getChildrenCloverDatabases();
            if (childrenDatabases.size() > 0) {
                // Ensure the merged database output directory exists
                new File(getCloverMergeDatabase()).getParentFile().mkdirs();

                // Merge the databases
                mergeCloverDatabases(childrenDatabases);
            } else {
                getLog().warn("No Clover databases found in children projects, no merge done. Run a build with debug logging for more details.");
            }
        }
    }

    private List<String> getChildrenCloverDatabases() {
        // Ideally we'd need to find out where each module stores its Clover
        // database. However that's not
        // currently possible in m2 (see
        // http://jira.codehaus.org/browse/MNG-2180). Thus we'll assume for now
        // that all modules use the cloverDatabase configuration from the top
        // level module.

        // Find out the location of the clover DB relative to the root module.
        // Note: This is a pretty buggy algorithm and we really need a proper
        // solution (see MNG-2180)
        final String resolvedCloverDb = resolveCloverDatabase();
        final String projectBaseDir = getProject().getBasedir().getPath();
        getLog().debug("Calculating relative database path of '" + resolvedCloverDb
                + "' against the project base directory '" + projectBaseDir + "'");
        final String relativeCloverDatabasePath = resolvedCloverDb.substring(projectBaseDir.length());
        getLog().debug("Relative path is '" + relativeCloverDatabasePath + "'");
        final List<String> dbFiles = new ArrayList<String>();
        final List<MavenProject> projects = getDescendantModuleProjects(getProject());

        for (MavenProject childProject : projects) {
            getLog().debug("Looking for Clover database for module " + childProject.getId() + " (" + childProject.getBasedir() + ")");
            final File cloverDb = new File(childProject.getBasedir(), relativeCloverDatabasePath);
            if (cloverDb.exists()) {
                getLog().debug("Database found at " + cloverDb.getAbsolutePath() + " . Adding for merge.");
                dbFiles.add(cloverDb.getPath());
            } else {
                getLog().debug("No database at " + cloverDb.getAbsolutePath());
            }
        }

        return dbFiles;
    }

    private void mergeCloverDatabases(final List<String> dbFiles) throws MojoExecutionException {
        final List<String> parameters = new ArrayList<String>();

        parameters.add("-s");
        parameters.add(span);

        parameters.add("-i");
        parameters.add(getCloverMergeDatabase());

        if (getLog().isDebugEnabled()) {
            parameters.add("-d");
        }

        parameters.addAll(dbFiles);

        int mergeResult = CloverMerge.mainImpl(Iterables.toArray(parameters, String.class));
        if (mergeResult != 0) {
            throw new MojoExecutionException("Clover has failed to merge the children module databases");
        }
    }
}
