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
import com.atlassian.clover.ant.tasks.HistoryPointTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.Project;

import java.io.File;

/**
 * Save a <a href="http://openclover.org/doc/manual/4.2.0/ant--tutorial-part-2-historical-reporting.html">Clover history point</a>.
 *
 * @goal save-history
 *
 */
public class CloverSaveHistoryMojo extends AbstractCloverMojo
{
    /**
     * The location where historical Clover data will be saved.
     *
     * <p>Note: It's recommended to modify the location of this directory so that it points to a more permanent
     * location as the <code>${project.build.directory}</code> directory is erased when the project is cleaned.</p>
     */
    @Parameter(property = "maven.clover.historyDir", defaultValue = "${project.build.directory}/clover/history", required = true)
    private String historyDir;

    /**
     * {@inheritDoc}
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        if (skip) {
            getLog().debug("Skipping clover save-history.");
            return;
        }

        // only save the history once, on the very last project.
        if (isSingleCloverDatabase() && !isLastProjectInReactor()) {
            getLog().info("Skipping Clover history point save until the final project in the reactor.");
            return;
        }
        
        if ( areCloverDatabasesAvailable() )
        {
            super.execute();

            AbstractCloverMojo.waitForFlush( getWaitForFlush(), getFlushInterval() );

            save();
        }
        else
        {
            getLog().info("No Clover database found, skipping the Clover history point save");
        }
    }

    /**
     * Save a history point for both the main Clover database and the merged Clover database when they exist.
     */
    private void save()
    {
        if ( new File( resolveCloverDatabase() ).exists() )
        {
            saveDatabase( resolveCloverDatabase() );
        }
        if ( new File( getCloverMergeDatabase() ).exists() )
        {
            saveDatabase( getCloverMergeDatabase() );
        }
    }

    /**
     * Save a history point for a Clover database.
     *
     * @param database the Clover database to save
     */
    private void saveDatabase(final String database)
    {
        final Project antProject = new Project();
        antProject.init();
        AbstractCloverMojo.registerCloverAntTasks(antProject, getLog());

        getLog().info( "Saving Clover history point for database [" + database + "] in ["
            + this.historyDir + "]" );

        HistoryPointTask cloverHistoryTask = createHistoryTask(antProject);
        cloverHistoryTask.init();
        cloverHistoryTask.setInitString( database );
        if (new File(this.historyDir).isAbsolute()) {
            cloverHistoryTask.setHistoryDir( new File(this.historyDir) );
        } else {
            cloverHistoryTask.setHistoryDir( new File(getProject().getBasedir(), this.historyDir ) );
        }
        executeTask(cloverHistoryTask);
    }

    protected void executeTask(HistoryPointTask cloverHistoryTask) {
        cloverHistoryTask.execute();
    }

    HistoryPointTask createHistoryTask(Project antProject) {
        return (HistoryPointTask) antProject.createTask( "clover-historypoint" );
    }
}
