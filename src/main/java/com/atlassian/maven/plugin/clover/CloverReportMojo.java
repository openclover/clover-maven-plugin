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

import com.atlassian.clover.cfg.Interval;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.atlassian.maven.plugin.clover.internal.AntPropertyHelper;
import com.atlassian.maven.plugin.clover.internal.CloverConfiguration;
import com.atlassian.maven.plugin.clover.internal.ConfigUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.PropertyHelper;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Generate a Clover report from existing Clover databases. The generated report
 * is an external report generated by Clover itself. If the project generating the report is a top level project and
 * if the <code>aggregate</code> configuration element is set to true then an aggregated report will also be created.
 *
 * <p>Note: This report mojo should be an @aggregator and the <code>clover:aggregate</code> mojo shouldn't exist. This
 * is a limitation of the site plugin which doesn't support @aggregator reports...</p>
 */
@Mojo(name = "clover")
public class CloverReportMojo extends AbstractMojo implements MavenReport, CloverConfiguration {

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private ArtifactResolver artifactResolver;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    /**
     * Use a custom report descriptor for generating your Clover Reports.
     * The format for the configuration file is identical to an Ant build file which uses the &lt;clover-report/&gt;
     * task. For a complete reference, please consult the:
     *  <a href="http://openclover.org/doc/manual/latest/maven--creating-custom-reports.html">Creating custom reports</a> and
     *  <a href="http://openclover.org/doc/manual/latest/ant--clover-report.html">clover-report documentation</a>
     */
    @Parameter(property = "maven.clover.reportDescriptor")
    private File reportDescriptor;

    /**
     * If set to true, the clover-report configuration file will be resolved as a versioned artifact by looking for it
     * in your configured maven repositories - both remote and local.
     */
    @Parameter(property = "maven.clover.resolveReportDescriptor", defaultValue = "false")
    private boolean resolveReportDescriptor;

    /**
     * Remote repositories used for the project.
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}")
    private List<ArtifactRepository> repositories;

    /**
     * The location of the <a href="http://openclover.org/doc/manual/latest/ant--managing-the-coverage-database.html">Clover database</a>.
     */
    @Parameter(property = "maven.clover.cloverDatabase")
    private String cloverDatabase;

    /**
     * If true, then a single database will be saved for the entire project, in the target directory of the execution
     * root.
     * If a custom location for the cloverDatabase is specified, this flag is ignored.
     */
    @Parameter(property = "maven.clover.singleCloverDatabase", defaultValue = "false")
    private boolean singleCloverDatabase;
    
    /**
     * The location of the merged clover database to create when running a report in a multimodule build.
     */
    @Parameter(property = "maven.clover.cloverMergeDatabase", defaultValue = "${project.build.directory}/clover/cloverMerge.db", required = true)
    private String cloverMergeDatabase;

    /**
     * The directory where the Clover report will be generated.
     */
    @Parameter(property = "maven.clover.outputDirectory", defaultValue = "${project.reporting.outputDirectory}/clover", required = true)
    private File outputDirectory;

    /**
     * <p>The location where historical Clover data will be saved.</p>
     * <p>Note: It's recommended to modify the location of this directory so that it points to a more permanent
     * location as the <code>${project.build.directory}</code> directory is erased when the project is cleaned.</p>
     */
    @Parameter(property = "maven.clover.historyDir", defaultValue = "${project.build.directory}/clover/history", required = true)
    private String historyDir;

    /**
     * When the Clover Flush Policy is set to "interval" or threaded this value is the minimum
     * period between flush operations (in milliseconds).
     */
    @Parameter(property = "maven.clover.flushInterval", defaultValue = "500")
    private int flushInterval;

    /**
     * If true we'll wait 2*flushInterval to ensure coverage data is flushed to the Clover database before running
     * any query on it.
     * <p/>
     * <p>Note: The only use case where you would want to turn this off is if you're running your tests in a separate
     * JVM. In that case the coverage data will be flushed by default upon the JVM shutdown and there would be no need
     * to wait for the data to be flushed. As we can't control whether users want to fork their tests or not, we're
     * offering this parameter to them.</p>
     */
    @Parameter(property = "maven.clover.waitForFlush", defaultValue = "true")
    private boolean waitForFlush;

    /**
     * Decide whether to generate an HTML report or not.
     */
    @Parameter(property = "maven.clover.generateHtml", defaultValue = "true")
    private boolean generateHtml;

    /**
     * Decide whether to generate a PDF report or not.
     */
    @Parameter(property = "maven.clover.generatePdf", defaultValue = "false")
    private boolean generatePdf;

    /**
     * Decide whether to generate a XML report or not.
     */
    @Parameter(property = "maven.clover.generateXml", defaultValue = "true")
    private boolean generateXml;

    /**
     * Decide whether to generate a JSON report or not.
     */
    @Parameter(property = "maven.clover.generateJson", defaultValue = "false")
    private boolean generateJson;

    /**
     * Decide whether to generate a Clover historical report or not.
     */
    @Parameter(property = "maven.clover.generateHistorical", defaultValue = "false")
    private boolean generateHistorical;

    /**
     * How to order coverage tables.
     */
    @Parameter(property = "maven.clover.orderBy", defaultValue = "PcCoveredAsc")
    private String orderBy;

    /**
     * Comma or space separated list of Clover somesrcexcluded (block, statement or method filers) to exclude when
     * generating coverage reports.
     */
    @Parameter(property = "maven.clover.contextFilters", defaultValue = "")
    private String contextFilters;

    /**
     * Specifies whether to include failed test coverage when calculating the total coverage percentage.
     *
     * @since 4.4.0
     */
    @Parameter(property = "maven.clover.includeFailedTestCoverage", defaultValue = "false")
    private boolean includeFailedTestCoverage;

    /**
     * Whether to show inner functions, i.e. functions declared inside methods in the report. This applies to Java8
     * lambda functions for instance. If set to <code>false</code> then they are hidden on the list of methods, but
     * code metrics still include them.
     *
     * Note: if you will use showLambdaFunctions=true and showInnerFunctions=false then only lambda functions declared
     * as a class field will be listed.
     *
     * @since 3.2.1
     */
    @Parameter(property = "maven.clover.showInnerFunctions", defaultValue = "false")
    private boolean showInnerFunctions;

    /**
     * Whether to show lambda functions in the report. Lambda functions can be either declared inside method body
     * or as a class field. If set to <code>false</code> then they are hidden on the list of methods, but code
     * metrics still include them.
     *
     * Note: if you will use showLambdaFunctions=true and showInnerFunctions=false then only lambda functions declared
     * as a class field will be listed.
     *
     * @since 3.2.1
     */
    @Parameter(property = "maven.clover.showLambdaFunctions", defaultValue = "false")
    private boolean showLambdaFunctions;

    /**
     * Calculate and show unique per-test coverage (for large projects, this can take a significant amount of time).
     *
     * @since 4.4.0
     */
    @Parameter(property = "maven.clover.showUniqueCoverage", defaultValue = "false")
    private boolean showUniqueCoverage;

    /**
     * Title of the report
     */
    @Parameter(property = "maven.clover.title", defaultValue = "${project.name} ${project.version}")
    private String title;
    
    /**
     * Title anchor of the report
     */
    @Parameter(property = "maven.clover.titleAnchor", defaultValue = "${project.url}")
    private String titleAnchor;

    /**
     * The charset to use in the html reports.
     */
    @Parameter(property = "maven.clover.charset", defaultValue = "UTF-8")
    private String charset;

    /**
     * <p>The Maven project instance for the executing project.</p>
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * <p>The projects in the reactor for aggregation report.</p>
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     */
    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    /**
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#licenseLocation
     */
    @Parameter(property = "maven.clover.licenseLocation")
    private String licenseLocation;

    /**
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#license
     */
    @Parameter(property = "maven.clover.license")
    private String license;

    /**
     * A span specifies the age of the coverage data that should be used when creating a report.
     */
    @Parameter(property = "maven.clover.span")
    private String span = Interval.DEFAULT_SPAN.toString();

    /**
     * If set to true, a report will be generated even in the absence of coverage data.
     */
    @Parameter(property = "maven.clover.alwaysReport", defaultValue = "true")
    private boolean alwaysReport = true;

    private static String nullToEmpty(String string) {
        return string != null ? string : "";
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            executeReport(Locale.ENGLISH);
        } catch (MavenReportException ex) {
            throw new MojoExecutionException("Failed to generate report", ex);
        }
    }

    // @Override till maven-reporting-api 3.0
    public void generate(org.codehaus.doxia.sink.Sink sink, Locale locale) throws MavenReportException {
        executeReport(locale);
    }

    // @Override since maven-reporting-api 3.1.0
    public void generate(org.apache.maven.doxia.sink.Sink sink, Locale locale) throws MavenReportException {
        executeReport(locale);
    }


    public void executeReport(final Locale locale) throws MavenReportException {
        if (!canGenerateReport()) {
            getLog().info("No report being generated for this module.");
        }

        // only run the report once, on the very last project.
        final MavenProject lastProject = getReactorProjects().get(getReactorProjects().size() - 1);
        final MavenProject thisProject = getProject();
        if (isSingleCloverDatabase() && !thisProject.equals(lastProject)) {
            getLog().info("Skipping report generation until the final project in the reactor.");
            return;
        }

        // Ensure the output directory exists
        this.outputDirectory.mkdirs();

        if (reportDescriptor == null) {
            reportDescriptor = resolveCloverDescriptor();
        } else if (!reportDescriptor.exists()){ // try finding this as a resource
            try {
                reportDescriptor = AbstractCloverMojo.getResourceAsFile(reportDescriptor.getPath(), getLog(), this.getClass().getClassLoader());
            } catch (MojoExecutionException e) {
                throw new MavenReportException("Could not resolve report descriptor: " + reportDescriptor.getPath(), e);
            }
        }

        getLog().info("Using Clover report descriptor: " + reportDescriptor.getAbsolutePath());

        if(title != null && title.startsWith("Unnamed")) { // no project.name on the project
            title = project.getArtifactId() + " " + project.getVersion();
        }

        File singleModuleCloverDatabase = new File(resolveCloverDatabase());
        if (singleModuleCloverDatabase.exists()) {
            createAllReportTypes(resolveCloverDatabase(), title);
        }

        File mergedCloverDatabase = new File(this.cloverMergeDatabase);
        if (mergedCloverDatabase.exists()) {
            createAllReportTypes(this.cloverMergeDatabase, title + " (Aggregated)");
        }
    }

    /**
     * Example of title prefixes: "Maven Clover", "Maven Aggregated Clover"
     */
    private void createAllReportTypes(final String database, final String titlePrefix) {

        final String outpath = outputDirectory.getAbsolutePath();
        if (this.generateHtml) {
            createReport(database, "html", titlePrefix, outpath, outpath, false);
        }
        if (this.generatePdf) {
            createReport(database, "pdf", titlePrefix, outpath + "/clover.pdf", outpath + "/historical.pdf", true);
        }
        if (this.generateXml) {
            createReport(database, "xml", titlePrefix, outpath + "/clover.xml", null, false);
        }
        if (this.generateJson) {
            createReport(database, "json", titlePrefix, outpath, null, false);
        }
    } 

    /**
     * Note: We use Clover's <code>clover-report</code> Ant task instead of the Clover CLI APIs because the CLI
     * APIs are limited and do not support historical reports.
     */
    private void createReport(final String database, final String format, final String title,
                              final String output, final String historyOut, final boolean summary) {
        final Project antProject = new Project();
        antProject.init();

        PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper( antProject );

        propertyHelper.setNext( new AntPropertyHelper( project, getLog() ) );

        antProject.setUserProperty("ant.file", reportDescriptor.getAbsolutePath());
        antProject.setCoreLoader(getClass().getClassLoader());

        addMavenProperties(antProject);
        
        antProject.setProperty("cloverdb", database);
        antProject.setProperty("output", output);
        antProject.setProperty("history", historyDir);
        antProject.setProperty("title", nullToEmpty(title)); // empty string will have it be ignore by clover
        antProject.setProperty("titleAnchor", nullToEmpty(titleAnchor));
        final String projectDir = project.getBasedir().getPath();
        antProject.setProperty("projectDir", projectDir);
        antProject.setProperty("testPattern", "**/src/test/**");
        antProject.setProperty("filter", nullToEmpty(contextFilters));
        antProject.setProperty("orderBy", orderBy);
        antProject.setProperty("charset", charset);
        antProject.setProperty("type", format);
        antProject.setProperty("span", span);
        antProject.setProperty("alwaysReport", Boolean.toString(alwaysReport));
        antProject.setProperty("summary", Boolean.toString(summary));
        antProject.setProperty("showInnerFunctions", Boolean.toString(showInnerFunctions));
        antProject.setProperty("showLambdaFunctions", Boolean.toString(showLambdaFunctions));
        antProject.setProperty("showUniqueCoverage", Boolean.toString(showUniqueCoverage));
        antProject.setProperty("includeFailedTestCoverage", Boolean.toString(includeFailedTestCoverage));
        if (historyOut != null) {
            antProject.setProperty("historyout", historyOut);
        }

        AbstractCloverMojo.registerCloverAntTasks(antProject, getLog());
        ProjectHelper.configureProject(antProject, reportDescriptor);
        antProject.setBaseDir(project.getBasedir());
        String target = (generateHistorical && isHistoricalDirectoryValid(output) && historyOut != null)
                ? "historical"
                : "current";
        antProject.executeTarget(target);
    }

    private void addMavenProperties(final Project antProject) {
        final Map<Object, Object> properties = getProject().getProperties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            getLog().debug("Setting Property: " + entry.getKey().toString() + " = " + entry.getValue().toString());
            antProject.setProperty(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    private boolean isHistoricalDirectoryValid(final String outFile) {
        boolean isValid = false;

        final File dir = new File(this.historyDir);
        if (dir.exists()) {
            final File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                isValid = true;
            } else if (generateHistorical){
                getLog().warn("No Clover historical data found in [" + this.historyDir + "], skipping Clover "
                        + "historical report generation ([" + outFile + "])");
            }
        } else if (generateHistorical){
            getLog().warn("Clover historical directory [" + this.historyDir + "] does not exist, skipping Clover "
                    + "historical report generation ([" + outFile + "])");
        }

        return isValid;
    }

    @Override
    public String getOutputName() {
        return "clover/index";
    }

    @Override
    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }

    @Override
    public File getReportOutputDirectory() {
        return this.outputDirectory.getAbsoluteFile();
    }

    @Override
    public String getDescription(final Locale locale) {
        return getBundle(locale).getString("report.clover.description");
    }

    private static ResourceBundle getBundle(final Locale locale) {
        return ResourceBundle.getBundle("clover-report", locale, CloverReportMojo.class.getClassLoader());
    }

    @Override
    public MavenProject getProject() {
        return this.project;
    }

    @Override
    public String getName(final Locale locale) {
        return getBundle(locale).getString("report.clover.name");
    }

    /**
     * Always return true as we're using the report generated by Clover rather than creating our own report.
     *
     * @return true
     */
    @Override
    public boolean isExternalReport() {
        return true;
    }

    /**
     * Generate reports if a Clover module database or a Clover merged database exist.
     *
     * @return true if a project should be generated
     */
    @Override
    public boolean canGenerateReport() {
        boolean canGenerate = false;

        AbstractCloverMojo.waitForFlush(this.waitForFlush, this.flushInterval);

        File singleModuleCloverDatabase = new File(resolveCloverDatabase());
        File mergedCloverDatabase = new File(this.cloverMergeDatabase);

        if (singleModuleCloverDatabase.exists() || mergedCloverDatabase.exists()) {
            if (this.generateHtml || this.generatePdf || this.generateXml) {
                canGenerate = true;
            }
        } else {
            getLog().warn("No Clover database found, skipping report generation");
        }

        return canGenerate;
    }

    @Override
    public void setReportOutputDirectory(final File reportOutputDirectory) {
        if ((reportOutputDirectory != null) && (!reportOutputDirectory.getAbsolutePath().endsWith("clover"))) {
            this.outputDirectory = new File(reportOutputDirectory, "clover");
        } else {
            this.outputDirectory = reportOutputDirectory;
        }
    }

    /**
     * The logic here is taken from AbstractSiteRenderingMojo#resolveSiteDescriptor in the maven-site-plugin.
     * See also: http://docs.codehaus.org/display/MAVENUSER/Mojo+Developer+Cookbook
     *
     * @return the clover report configuration file to use
     * @throws MavenReportException if at least the default file can't be resolved
     */
    protected File resolveCloverDescriptor()
            throws MavenReportException {

        if (resolveReportDescriptor) {
            getLog().info("Attempting to resolve the clover-report configuration as an xml artifact.");
            final Artifact artifact = repositorySystem.createArtifactWithClassifier(
                    project.getGroupId(),
                    project.getArtifactId(),
                    project.getVersion(),
                    "xml", "clover-report");

            try {
                final ArtifactResult result = artifactResolver.resolveArtifact(mavenSession.getProjectBuildingRequest(), artifact);
                return result.getArtifact().getFile();
            } catch (ArtifactResolverException e) {
                getLog().warn("Failed to resolve artifact " + artifact);
            }
        }

        try {
            getLog().info("Using default-clover-report descriptor.");
            final File file = AbstractCloverMojo.getResourceAsFile(
                    "default-clover-report.xml",
                    getLog(),
                    this.getClass().getClassLoader());
            file.deleteOnExit();
            return file;

        } catch (Exception e) {
            throw new MavenReportException("Could not resolve default-clover-report.xml. " +
                    "Please try specifying this via the maven.clover.reportDescriptor property.", e);
        }
    }

    @Override
    public String getCloverDatabase()
    {
        return cloverDatabase;
    }

    @Override
    public String resolveCloverDatabase()
    {
        return new ConfigUtil(this).resolveCloverDatabase();
    }

    @Override
    public List<MavenProject> getReactorProjects() {
        return reactorProjects;
    }

    @Override
    public boolean isSingleCloverDatabase() {
        return this.singleCloverDatabase;
    }

}
