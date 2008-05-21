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

import com.cenqua.clover.cfg.Percentage;
import com.cenqua.clover.tasks.CloverPassTask;
import org.apache.maven.plugin.MojoExecutionException;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.File;

/**
 * Verify Test Percentage Coverage (TPC) from an existing Clover database and fail the build if it is below the defined
 * threshold. The check is done on main Clover databases and also on merged Clover databases when they exist.
 *
 * @goal check
 * @phase verify
 *
 * @version $Id: CloverCheckMojo.java 555822 2007-07-13 00:03:28Z vsiveton $
 */
public class CloverCheckMojo extends AbstractCloverMojo
{
    /**
     * The Test Percentage Coverage (TPC) threshold under which the plugin will report an error and fail the build.
     *
     * @parameter expression="${maven.clover.targetPercentage}" 
     */
    private String targetPercentage;

    /**
     * Comma or space separated list of Clover contexts (block, statement or method filers) to exclude when
     * generating coverage reports.
     * @parameter expression="${maven.clover.contextFilters}"
     */
    private String contextFilters;

    /**
     * Do we fail the build on a violation? The default is true but there are some edge cases where you want to be
     * able to check what would fail but without actually failing the build. For example you may want to let the build
     * continue so that you can verify others checks that are executed after the Clover checks. 
     *
     * @parameter expression="${maven.clover.failOnViolation}" default-value="true"
     */
    private boolean failOnViolation;


    /**
     * The location where historical Clover data is located.
     * <p/>
     * <p>
     * Allows you to specify a location for historical build data, along with a configurable threshold expressed as a percentage ?
     * used to cause the build to fail if coverage has dropped.
     * This attribute is passed down to specified packages, then the same test is done for these at the package level.
     * This will only be used if there is no targetPercentage parameter set.
     *
     * @parameter expression="${maven.clover.historyDir}" default-value="${project.build.directory}/clover/history"
     */
    private File historyDir;

    /**
     * The percentage threshold to use if clover-check is checking coverage against historical clover data.
     *
     * This is the amount of leeway to use when comparing the current build's coverage with that of the last build.
     * 
     * @parameter expression="${maven.clover.historyThreshold}" default-value="0%"
     *
     */
    private String historyThreshold;

    /**
     * {@inheritDoc}
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     * @throws MojoExecutionException when the TPC is below the threshold
     */
    public void execute()
        throws MojoExecutionException
    {

        if (skip) {
            getLog().debug("Skipping clover check.");
            return;
        }

        if ( !isInCloverForkedLifecycle() )
        {
            if ( areCloverDatabasesAvailable() )
            {
                super.execute();

                AbstractCloverMojo.waitForFlush( getWaitForFlush(), getFlushInterval() );

                check();
            }
            else
            {
                getLog().info("No Clover database found, skipping test coverage verification");
            }
        }
    }

    /**
     * Check both the main Clover database and the merged Clover database when they exist.
     * @throws MojoExecutionException when the TPC is below the threshold
     */
    private void check() throws MojoExecutionException
    {
        if ( new File( getCloverDatabase() ).exists() )
        {
            checkDatabase( getCloverDatabase() );
        }
        if ( new File( getCloverMergeDatabase() ).exists() )
        {
            checkDatabase( getCloverMergeDatabase() );
        }
    }

    /**
     * Check a Clover database and fail the build if the TPC is below the threshold.
     *
     * @param database the Clover database to verify
     * @throws MojoExecutionException when the TPC is below the threshold
     */
    private void checkDatabase(String database) throws MojoExecutionException
    {
        final Project antProject = new Project();
        antProject.init();
        AbstractCloverMojo.registerCloverAntTasks(antProject, getLog());

        CloverPassTask cloverPassTask = (CloverPassTask) antProject.createTask( "clover-check" );
        cloverPassTask.init();
        cloverPassTask.setInitString( database );
        cloverPassTask.setHaltOnFailure( true );
        cloverPassTask.setFailureProperty( "clovercheckproperty" );

        if (this.targetPercentage != null) {
            cloverPassTask.setTarget( new Percentage( this.targetPercentage ) );
            getLog().info( "Checking for coverage of [" + targetPercentage + "] for database [" + database + "]");            
        } else {
            cloverPassTask.setHistorydir(this.historyDir);
            cloverPassTask.setThreshold(new Percentage(this.historyThreshold));
            getLog().info( "Checking coverage against historical data [" +
                            this.historyDir + " +/-" + this.historyThreshold +
                           " ] for database [" + database + "]");
        }

        if ( this.contextFilters != null )
        {
            cloverPassTask.setFilter( this.contextFilters );
        }

        try
        {
            cloverPassTask.execute();
        }
        catch ( BuildException e )
        {
            getLog().error( antProject.getProperty( "clovercheckproperty" ) );

            if ( this.failOnViolation )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            else
            {
                getLog().warn( "Clover test percentage coverage is below threshold but failOnViolation is set to "
                    + " false, preventing the build from failing." );
            }
        }
    }

    /**
     * @return true if the build is currently inside the custom build lifecycle forked by the
     *         <code>clover:instrument</code> MOJO.
     */
    private boolean isInCloverForkedLifecycle()
    {
        // We know we're in the forked lifecycle if the output directory is set to target/clover...
        // TODO: Not perfect, need to find a better way. This is a hack!
        return getProject().getBuild().getDirectory().endsWith( "clover" );
    }
}
