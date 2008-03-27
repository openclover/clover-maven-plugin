package com.atlassian.maven.plugin.clover.internal;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Taskdef;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;

import java.io.File;

import com.cenqua.clover.CloverNames;
import com.atlassian.maven.plugin.clover.MvnLogBuildListener;

/**
 * Common code for all Clover plugin build Mojos.
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id: AbstractCloverMojo.java 555822 2007-07-13 00:03:28Z vsiveton $
 */
public abstract class AbstractCloverMojo extends AbstractMojo
{
    /**
     * The location of the <a href="http://cenqua.com/clover/doc/adv/database.html">Clover database</a>.
     *
     * @parameter expression="${maven.clover.cloverDatabase}" default-value="${project.build.directory}/clover/clover.db"
     */
    private String cloverDatabase;

    /**
     * The location of the merged clover database to create when running a report in a multimodule build.
     *
     * @parameter expression="${maven.clover.cloverMergeDatabase}" default-value="${project.build.directory}/clover/cloverMerge.db"
     * @required
     */
    private String cloverMergeDatabase;

    /**
     * A Clover license file to be used by the plugin. The plugin tries to resolve this parameter first as a resource,
     * then as a URL, and then as a file location on the filesystem.
     *
     * A trial Clover license can be generated <a href="http://www.atlassian.com/ex/GenerateLicense.jspa?product=Clover&version=2">here</a>.
     * @see #license
     * @parameter expression="${maven.clover.licenseLocation}"
     */
    protected String licenseLocation;

    /**
     * The full Clover license String to use. If supplied, this certificate will be used over {@link #licenseLocation}.
     * NB. newline chars must be preserved.
     * A trial Clover license can be generated <a href="http://www.atlassian.com/ex/GenerateLicense.jspa?product=Clover&version=2">here</a>.
     *
     * @see #licenseLocation
     * @parameter expression="${maven.clover.license}"
     */
    protected String license;


    /**
     * The <a href="http://cenqua.com/clover/doc/adv/flushpolicies.html">Clover flush policy</a> to use.
     * Valid values are <code>directed</code>, <code>interval</code> and <code>threaded</code>.
     *
     * @parameter expression="${maven.clover.flushPolicy}" default-value="threaded"
     */
    private String flushPolicy;

    /**
     * When the Clover Flush Policy is set to "interval" or threaded this value is the minimum period between flush
     * operations (in milliseconds).
     *
     * @parameter expression="${maven.clover.flushInterval}" default-value="500"
     */
    private int flushInterval;

    /**
     * If true we'll wait 2*flushInterval to ensure coverage data is flushed to the Clover database before running
     * any query on it.
     *
     * <p>Note: The only use case where you would want to turn this off is if you're running your tests in a separate
     * JVM. In that case the coverage data will be flushed by default upon the JVM shutdown and there would be no need
     * to wait for the data to be flushed. As we can't control whether users want to fork their tests or not, we're
     * offering this parameter to them.</p>
     *
     * @parameter expression="${maven.clover.waitForFlush}" default-value="true"
     */
    private boolean waitForFlush;

    /**
     * If true we won't instrument the tests to record results, and will get test results from the
     * surefire test results instead.
     *
     * @parameter expression="${maven.clover.useSurefireTestResults}" default-value="false"
     */
    private boolean useSurefireTestResults;

    /**
     * Whether the Clover instrumentation should use the Clover <code>jdk14</code> or <code>jdk15</code> flags to
     * parse sources.
     *
     * @parameter expression="${maven.clover.jdk}"
     */
    private String jdk;

    /**
     * The Maven project instance for the executing project.
     *
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * Resource manager used to locate any Clover license file provided by the user.
     * @component
     */
    private ResourceManager resourceManager;

    /**
     *
     * A flag to indicate not to run clover for this execution.
     *
     * If set to true, Clover will not be run.
     * 
     * @parameter expression="${maven.clover.skip}" default-value="false"
     */
    protected boolean skip;

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException
    {
        registerLicenseFile();
    }

    public void setResourceManager(ResourceManager resourceManager)
    {
        this.resourceManager = resourceManager;
    }

    public ResourceManager getResourceManager()
    {
        return this.resourceManager;
    }

    protected void registerLicenseFile() throws MojoExecutionException
    {
        AbstractCloverMojo.registerLicenseFile(
                    this.project,
                    getResourceManager(),
                    this.licenseLocation,
                    getLog(),
                    this.getClass().getClassLoader(),
                    this.license);
    }

    /**
     * Registers the license file for Clover runtime by setting the <code>clover.license.path</code> system property.
     * If the user has configured the <code>licenseLocation</code> parameter the plugin tries to resolve it first as a
     * resource, then as a URL, and then as a file location on the filesystem. If the <code>licenseLocation</code>
     * parameter has not been defined by the user we look up a default Clover license in the classpath in
     * <code>/clover.license</code>.
     * </p>
     * Note: We're defining this method as static because it is also required in the report mojo and reporting mojos
     * and main mojos cannot share anything right now. See http://jira.codehaus.org/browse/MNG-1886.
     *
     * @throws MojoExecutionException when the license file cannot be found
     */
    public static void registerLicenseFile(MavenProject project,
                                           ResourceManager resourceManager,
                                           String licenseLocation,
                                           Log logger,
                                           ClassLoader classloader,
                                           String licenseCert) throws MojoExecutionException
    {

        if (licenseCert != null) {
            logger.debug("Full license supplied '" + licenseCert +
                        "'. License location: '" + licenseLocation + "' will be ignored.");            
            System.setProperty(CloverNames.PROP_LICENSE_CERT, licenseCert);
            return;
        }
        
        logger.debug("Using licenseLocation '" + licenseLocation +"'");

        resourceManager.addSearchPath( "url", "" );
        resourceManager.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );

        String licenseFile;
        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();        
        try {
            Thread.currentThread().setContextClassLoader(classloader);
            if (licenseLocation == null) {
                logger.info("No 'maven.clover.licenseLocation' configured. Using default evaluation license.");
                licenseLocation = "/clover.license";
            }

            try {
                logger.debug("Attempting to load license from [" + licenseLocation + "] ...");
                licenseFile = resourceManager.getResourceAsFile(licenseLocation).getPath();
                logger.info("License loaded from: '"+ licenseFile + "'");
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to load license file [" + licenseLocation + "]", e);
            }

            logger.debug("Using license file [" + licenseFile + "]");
            System.setProperty(CloverNames.PROP_LICENSE_PATH, licenseFile);
        }
        finally {
            Thread.currentThread().setContextClassLoader( origLoader );
        }
    }

    /**
     * Register the Clover Ant tasks against a fake Ant {{@link Project}} object so that we can the tasks later on.
     * This is the Java equivalent of the <code>taskdef</code> call that you would need in your Ant
     * <code>build.xml</code> file if you wanted to use the Clover Ant tasks from Ant.
     * </p>
     * Note: We're defining this method as static because it is also required in the report mojo and reporting mojos
     * and main mojos cannot share anything right now. See http://jira.codehaus.org/browse/MNG-1886.
     *
     */
    public static void registerCloverAntTasks(Project antProject, Log log)
    {
        antProject.addBuildListener(new MvnLogBuildListener(log));
        Taskdef taskdef = (Taskdef) antProject.createTask( "taskdef" );
        taskdef.init();
        taskdef.setResource( "cloverlib.xml" );
        taskdef.execute();
    }

    /**
     * Wait 2*'flush interval' milliseconds to ensure that the coverage data have been flushed to the Clover database.
     *
     * TODO: This method should not be static but we need it static here because we cannot share code
     * between non report mojos and main build mojos. See http://jira.codehaus.org/browse/MNG-1886
     */
    public static void waitForFlush(boolean waitForFlush, int flushInterval)
    {
        if ( waitForFlush )
        {
            try
            {
                Thread.sleep( 2 * flushInterval );
            }
            catch ( InterruptedException e )
            {
                // Nothing to do... Just go on and try to check for coverage.
            }
        }
    }

    /**
     * Check if a Clover database exists (either a single module Clover database or an aggregated one).
     * @return true if a Clover database exists.
     */
    protected boolean areCloverDatabasesAvailable()
    {
        boolean shouldRun = false;

        File singleModuleCloverDatabase = new File( this.cloverDatabase );
        File mergedCloverDatabase = new File ( this.cloverMergeDatabase );

        if (singleModuleCloverDatabase.exists() || mergedCloverDatabase.exists() )
        {
            shouldRun = true;
        }

        return shouldRun;
    }

    public MavenProject getProject()
    {
        return this.project;
    }

    public boolean getWaitForFlush()
    {
        return this.waitForFlush;
    }

    public String getJdk()
    {
        return this.jdk;
    }

    public String getCloverDatabase()
    {
        return this.cloverDatabase;
    }

    protected String getCloverMergeDatabase()
    {
        return this.cloverMergeDatabase;
    }

    public int getFlushInterval()
    {
        return this.flushInterval;
    }

    public String getFlushPolicy()
    {
        return this.flushPolicy;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public boolean isUseSurefireTestResults() {
        return useSurefireTestResults;
    }

    public void setLicenseLocation(String licenseLocation) {
        this.licenseLocation = licenseLocation;
    }

    public void setLicense(String license) {
        this.license = license;
    }
}
