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
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.atlassian.clover.util.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Taskdef;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.atlassian.maven.plugin.clover.MvnLogBuildListener;

/**
 * Common code for all Clover plugin build Mojos.
 */
public abstract class AbstractCloverMojo extends AbstractMojo implements CloverConfiguration {
    /**
     * The directory where the Clover plugin will put all the files it generates during the build process. For
     * example the Clover plugin will put instrumented sources somewhere inside this directory.
     */
    @Parameter(defaultValue = "${project.build.directory}/clover", required = true)
    protected String cloverOutputDirectory;

    /**
     * The location of the <a href="https://openclover.org/doc/manual/latest/ant--managing-the-coverage-database.html">Clover database</a>.
     */
    @Parameter(property = "maven.clover.cloverDatabase")
    private String cloverDatabase;

    /**
     * The location to store the clover snapshot file. This file needs to persist between builds to enable Clover's
     * build optimization feature. If not specified, the snapshot will be stored next to the cloverDatabase.
     */
    @Parameter(property = "maven.clover.snapshot")
    protected File snapshot;

    /**
     * If true, then a single cloverDatabase will be used for the entire project.
     * This flag will be ignored if a custom cloverDatabase location is specified.
     */
    @Parameter(property = "maven.clover.singleCloverDatabase", defaultValue = "false")
    private boolean singleCloverDatabase;

    /**
     * The location of the merged clover database to create when running a report in a multimodule build.
     */
    @Parameter(property = "maven.clover.cloverMergeDatabase", defaultValue = "${project.build.directory}/clover/cloverMerge.db", required = true)
    private String cloverMergeDatabase;

    /**
     * A Clover license file to be used by the plugin. The plugin tries to resolve this parameter first as a resource,
     * then as a URL, and then as a file location on the filesystem. If not provided, Clover will use a bundled
     * license key.
     *
     * @see #license
     */
    @Parameter(property = "maven.clover.licenseLocation")
    protected String licenseLocation;

    /**
     * The full Clover license String to use. If supplied, this certificate will be used over {@link #licenseLocation}.
     * NB. newline chars must be preserved. If not provided, Clover will use a bundled license key.
     *
     * @see #licenseLocation
     */
    @Parameter(property = "maven.clover.license")
    protected String license;

    /**
     * When the Clover Flush Policy is set to "interval" or threaded this value is the minimum period between flush
     * operations (in milliseconds).
     */
    @Parameter(property = "maven.clover.flushInterval", defaultValue = "500")
    private int flushInterval;

    /**
     * <p>If true we'll wait 2*flushInterval to ensure coverage data is flushed to the Clover database before running
     * any query on it.</p>
     * <p>Note: The only use case where you would want to turn this off is if you're running your tests in a separate
     * JVM. In that case the coverage data will be flushed by default upon the JVM shutdown and there would be no need
     * to wait for the data to be flushed. As we can't control whether users want to fork their tests or not, we're
     * offering this parameter to them.</p>
     */
    @Parameter(property = "maven.clover.waitForFlush", defaultValue = "true")
    private boolean waitForFlush;

    /**
     * <p>The Maven project instance for the executing project.</p>
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     */
    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    /**
     * A flag to indicate not to run clover for this execution. If set to true, Clover will not be run.
     */
    @Parameter(property = "maven.clover.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * If you wish to enable debug level logging in just the Clover plugin, set this to true. This is useful for
     * integrating Clover into the build.
     */
    @Parameter(property = "maven.clover.debug", defaultValue = "false")
    protected boolean debug;

    /**
     * <p>The projects in the reactor for aggregation report.</p>
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     */
    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;


    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException {
    }


    public static File getResourceAsFile(final String resourceLocation,
                                         final Log logger,
                                         final ClassLoader classloader) throws MojoExecutionException {

        logger.debug("Getting resource: '" + resourceLocation + "'");

        try {
            logger.debug("Attempting to load resource from [" + resourceLocation + "] ...");
            final File outputFile = File.createTempFile("mvn", "resource");
            outputFile.deleteOnExit();
            FileUtils.resourceToFile(classloader, resourceLocation, outputFile);
            return outputFile;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to load resource as file [" + resourceLocation + "]", e);
        }
    }

    /**
     * Register the Clover Ant tasks against a fake Ant {{@link Project}} object so that we can the tasks later on.
     * This is the Java equivalent of the <code>taskdef</code> call that you would need in your Ant
     * <code>build.xml</code> file if you wanted to use the Clover Ant tasks from Ant.
     *
     * Note: We're defining this method as static because it is also required in the report mojo and reporting mojos
     * and main mojos cannot share anything right now. See <a href="https://jira.codehaus.org/browse/MNG-1886">MNG-1886</a>.
     *
     * @param antProject project
     * @param log logger
     */
    public static void registerCloverAntTasks(final Project antProject, final Log log) {
        antProject.addBuildListener(new MvnLogBuildListener(log));
        final Taskdef taskdef = (Taskdef) antProject.createTask("taskdef");
        taskdef.init();
        taskdef.setResource("cloverlib.xml");
        taskdef.execute();
    }

    /**
     * Wait 2*'flush interval' milliseconds to ensure that the coverage data have been flushed to the Clover database.
     *
     * This method should not be static, but we need it static here because we cannot share code
     * between non report mojos and main build mojos.
     *
     * @param waitForFlush whether to pause until flush occurs
     * @param flushInterval current interval
     */
    public static void waitForFlush(final boolean waitForFlush, final int flushInterval) {
        if (waitForFlush) {
            try {
                Thread.sleep(2L * flushInterval);
            } catch (InterruptedException e) {
                // Nothing to do... Just go on and try to check for coverage.
            }
        }
    }

    /**
     * Check if a Clover database exists (either a single module Clover database or an aggregated one).
     *
     * @return true if a Clover database exists.
     */
    protected boolean areCloverDatabasesAvailable() {
        boolean shouldRun = false;
        final File singleModuleCloverDatabase = new File(resolveCloverDatabase());
        final File mergedCloverDatabase = new File(this.cloverMergeDatabase);

        if (singleModuleCloverDatabase.exists() || mergedCloverDatabase.exists()) {
            shouldRun = true;
        }

        return shouldRun;
    }

    public MavenProject getProject() {
        return this.project;
    }

    public boolean getWaitForFlush() {
        return this.waitForFlush;
    }

    public String getCloverDatabase() {
        return cloverDatabase;
    }

    public String resolveCloverDatabase() {
        return new ConfigUtil(this).resolveCloverDatabase();
    }

    protected String getCloverMergeDatabase() {
        return this.cloverMergeDatabase;
    }

    public int getFlushInterval() {
        return this.flushInterval;
    }

    public void setProject(final MavenProject project) {
        this.project = project;
    }

    public void setLicenseLocation(final String licenseLocation) {
        this.licenseLocation = licenseLocation;
    }

    public void setLicense(final String license) {
        this.license = license;
    }

    public List<MavenProject> getReactorProjects() {
        return reactorProjects;
    }

    public boolean isSingleCloverDatabase() {
        return this.singleCloverDatabase;
    }

    protected boolean isLastProjectInReactor() {
        final MavenProject lastProject = getReactorProjects().get(getReactorProjects().size() - 1);
        final MavenProject thisProject = getProject();
        return thisProject.equals(lastProject);
    }

    /**
     * Returns true if the supplied potentialModule project is a module
     * of the specified parentProject.
     *
     * @param parentProject   the parent project.
     * @param potentialModule the potential moduleproject.
     * @return true if the potentialModule is indeed a module of the specified
     *         parent project.
     */
    protected boolean isModuleOfProject(final MavenProject parentProject, final MavenProject potentialModule) {
        boolean result = false;
        final List<String> modules = parentProject.getModules();

        if (modules != null) {
            final File parentBaseDir = parentProject.getBasedir();

            for (final String module : modules) {
                File moduleBaseDir = new File(parentBaseDir, module);

                try {
                    // need these to be canonical paths, so we can perform a true equality
                    // operation and remember <module> is a path and for flat multi-module project
                    // structures they will be like this: <module>../a-project<module>
                    final String lhs = potentialModule.getBasedir().getCanonicalPath();
                    final String rhs = moduleBaseDir.getCanonicalPath();

                    if (lhs.equals(rhs)) {
                        getLog().debug("isModuleOfProject: lhs=" + lhs + " rhs=" + rhs + " MATCH FOUND");
                        result = true;
                        break;
                    } else {
                        getLog().debug("isModuleOfProject: lhs=" + lhs + " rhs=" + rhs);
                    }
                } catch (IOException e) {
                    // suppress the exception (?)
                    getLog().error("error encountered trying to resolve canonical module paths");
                }
            }
        }

        return result;
    }

    /**
     * Returns all the projects that are modules, or modules of modules, of the
     * specified project found within the reactor.
     *
     * The searchLevel parameter controls how many descendent levels of modules
     * are returned. With a searchLevels equals to 1, only the immediate modules
     * of the specified project are returned.
     *
     * A searchLevel equals to 2 returns those module's modules as well.
     *
     * A searchLevel equals to -1 returns the entire module hierarchy beneath the
     * specified project. Note that this is simply the equivalent to the entire reactor
     * if the specified project is the root execution project.
     *
     * @param project the project to search under
     * @param levels  the number of descendent levels to return (List&lt;MavenProject&gt;)
     * @return the list of module projects.
     */
    protected List<MavenProject> getModuleProjects(final MavenProject project, final int levels) {
        final List<MavenProject> projects = new ArrayList<>();
        final boolean infinite = (levels == -1);

        getLog().debug("getModuleProjects: project=" + project.getId()
                + " getReactorProjects is " + (getReactorProjects() == null ? "null" : "not null")
                + " infinite=" + infinite + " levels=" + levels);

        if ((getReactorProjects() != null) && (infinite || levels > 0)) {
            for (final MavenProject reactorProject : getReactorProjects()) {
                getLog().debug("getModuleProjects: checking " + reactorProject.getId() + " against " + project.getId());
                if (isModuleOfProject(project, reactorProject)) {
                    getLog().debug("getModuleProjects: reactor project " + reactorProject.getId() + " is a module of " + project.getId());
                    projects.add(reactorProject);
                    if (project == reactorProject) {
                        projects.add(project); //CLMVN-78 don't recurse if project is the same as reactorProject.
                    } else {
                        projects.addAll(getModuleProjects(reactorProject,
                                infinite ? levels : levels - 1));
                    }
                } else {
                    getLog().debug("getModuleProjects: reactor project " + reactorProject.getId() + " is not a module of " + project.getId());
                }
            }
        }

        return projects;
    }

    /**
     * returns all the projects that are in the reactor build as
     * direct or indirect modules of the specified project.
     *
     * @param project the project to search beneath
     * @return the list of modules that are direct or indirect module descendants (List&lt;MavenProject&gt;)
     *         of the specified project
     */
    protected List<MavenProject> getDescendantModuleProjects(final MavenProject project) {
        getLog().debug("Getting descendant module projects for " + project);
        return getModuleProjects(project, -1);
    }
}
