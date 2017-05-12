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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;


import java.io.File;

import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.ant.tasks.CloverMergeTask;

/**
 * Merge arbitrary number of clover databases into one.
 *
 * All databases must share a common root directory.
 * Thankyou to Alex B.(dasmonsieur@gmail.com) for providing the basis of this
 * mojo via: http://developer.atlassian.com/jira/browse/CLMVN-36
 *
 * @goal merge
 *
 */
public class CloverMergeMojo extends AbstractCloverMojo
{

    /**
     * Root directory with clover databases to merge
     *
     * @parameter expression="${maven.clover.merge.basedir}"
     * @required
     */
    private File baseDir;

    /**
     * Java pattern of clover database file name endings to merge.
     *
     * Patterns may be separated by a comma or a space.
     *
     * @parameter expression="${maven.clover.merge.includes}" default-value="*.db"
     */
    private String includes;

    /**
     * How far back to load coverage recordings from when merging
     *
     * @parameter expression="${maven.clover.merge.span}"
     */
    private String span;

 

    /**
     * {@inheritDoc}
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute() throws MojoExecutionException
    {

        // Ensure all databases are flushed
        AbstractCloverMojo.waitForFlush(getWaitForFlush(), getFlushInterval());
        mergeCloverDatabases();

    }

    private void mergeCloverDatabases() throws MojoExecutionException
    {

        AbstractCloverMojo.registerLicenseFile(getProject(), getResourceManager(),
                                               this.licenseLocation, getLog(),
                                               this.getClass().getClassLoader(),
                                               this.license);

        try {
            final Project antProject = new Project();
            antProject.init();
            CloverMergeTask merge = new CloverMergeTask();
            merge.setProject(antProject);
            merge.init();
            merge.setInitString(getCloverMergeDatabase());

            CloverMergeTask.CloverDbSet dbSet = new CloverMergeTask.CloverDbSet();
            dbSet.setProject(antProject);
            dbSet.setIncludes(includes);
            dbSet.setDir(baseDir);
            if (span != null) {
                dbSet.setSpan(new Interval(span));
            }

            merge.addCloverDbSet(dbSet);

            merge.execute();
        } catch (BuildException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
