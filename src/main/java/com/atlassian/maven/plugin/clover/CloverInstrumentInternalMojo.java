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

import com.atlassian.clover.ant.groovy.GroovycSupport;
import com.atlassian.clover.instr.java.InstrumentationConfig;
import com.atlassian.clover.remote.DistributedConfig;
import com.atlassian.maven.plugin.clover.internal.scanner.GroovySourceScanner;
import com.atlassian.maven.plugin.clover.internal.scanner.GroovyTestScanner;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;
import com.atlassian.maven.plugin.clover.internal.instrumentation.MainInstrumenter;
import com.atlassian.maven.plugin.clover.internal.instrumentation.TestInstrumenter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * <p>Instrument source roots.</p>
 *
 * <p><b>Note 1: Do not call this MOJO directly. It is meant to be called in a custom forked lifecycle by the other
 * Clover plugin MOJOs.</b></p>
 * <p><b>Note 2: We bind this mojo to the "validate" phase so that it executes prior to any other mojos</b></p>
 *
 * @goal instrumentInternal
 * @phase validate
 * @requiresDependencyResolution test
 *
 */
public class CloverInstrumentInternalMojo extends AbstractCloverMojo implements CompilerConfiguration
{

    /**
     * List of all artifacts for this Clover plugin provided by Maven. This is used internally to get a handle on
     * the Clover JAR artifact.
     *
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     */
    private List pluginArtifacts;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * Artifact resolver used to find clovered artifacts (artifacts with a clover classifier).
     *
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    private ArtifactResolver artifactResolver;

    /**
     * Local maven repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository localRepository;
    
    /**
     * Remote repositories used for the project.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    protected List repositories;



    /**
     * The list of file to include in the instrumentation.
     * Defaults are '**&#47;.java, **&#47;*.groovy' which are overwritten if &lt;includes&gt; is set by the user
     *
     * @parameter
     */
    private Set includes = new HashSet(Arrays.asList(new String[]{"**/*.java", "**/*.groovy"}));

    /**
     * The comma seperated list of files to include in the instrumentation.
     * Defaults are **.java which are overwritten if &lt;includes&gt; is set by the user
     *
     * @parameter expression="${maven.clover.includesList}"
     */
    private String includesList = null;

    /**
     * The comma seperated list of file to exclude from the instrumentation.
     * @parameter expression="${maven.clover.excludesList}" 
     */
    private String excludesList = null;


    /**
     * The list of file to exclude from the instrumentation.
     * @parameter
     */
    private Set excludes = new HashSet();

    /**
     * Specifies the custom method contexts to use for filtering specific methods from Clover reports.
     *
     * e.g. <pre>&lt;main&gt;public static void main\(String args\[\]\).*&lt;/main&gt;</pre>
     * will define the context called 'main' which will match all public static void main methods.
     *
     * @parameter 
     */
    private Map methodContexts = new HashMap();

    /**
     * Specifies the custom statement contexts to use for filtering specific statements from Clover reports.
     *
     * e.g.<pre>&lt;log&gt;^LOG\..*&lt;/log&gt;<pre>
     * defines a statement context called "log" which matches all LOG statements.
     * 
     * @parameter
     */
    private Map statementContexts = new HashMap();

    /**
     * Whether the Clover plugin should instrument all source roots (ie even
     * generated sources) or whether it should only instrument the main source
     * root.
     * @parameter expression="${maven.clover.includesAllSourceRoots}" default-value="false"
     */
    private boolean includesAllSourceRoots;

    /**
     * Whether the Clover plugin should instrument test source roots.
     * @parameter  expression="${maven.clover.includesTestSourceRoots}" default-value="true"
     */
    private boolean includesTestSourceRoots;

    /**
     * Use the fully qualified package name for java.lang.* classes.
     *
     * @parameter expression="${maven.clover.useFullyQualifiedJavaLang}" default-value="true"
     */
    private boolean useFullyQualifiedJavaLang;

    /**
     * Whether or not to include the -clover classifier on artifacts.
     *
     * @parameter expression="${maven.clover.useCloverClassifier}" default-value="true"
     */
    private boolean useCloverClassifier = true;

    /**
     * The character encoding to use when parsing source files.
     *
     * @parameter expression="${maven.clover.encoding}" 
     */
    private String encoding;


    /**
     * Sets the granularity in milliseconds of the last modification date for testing whether a source needs reinstrumentation.
     *
     * @parameter expression="${maven.clover.staleMillis}" default-value=0
     */
    private int staleMillis;

    /**
     * The configuration for distributed coverage collection by Clover.
     *
     * If present, default values will be used and coverage will be collected across JVMs.
     *
     * Optional nested elements (and their defaults) of distributedCoverage are:
     *  <ul>
     *   <li><tt>host</tt> - the host name of the JVM running the tests. default: <b>localhost</b></li>
     *   <li><tt>port</tt> - the port that Clover can bind to in the host JVM. default: <b>1198</b></li>
     *   <li><tt>numClients</tt> - the number of clients expected to attach to the Test JVM. The test JVM will wait until numClients
     *                    have connected before continuing. default: <b>0</b></li>
     *   <li><tt>timeout</tt> - the amount of time to wait for a response from a remote JVM before shunning it. default: <b>5000</b></li>
     *   <li><tt>retryPeriod</tt> - the amount of time a client should wait between reconnect attempts. default: <b>1000</b></li>
     *  </ul>
     *
     * @parameter 
     */
    private DistributedCoverage distributedCoverage;

    /**
     * The level to instrument to. Valid values are 'method' or 'statement'. Default is 'statement'.
     *
     * Setting this to 'method' greatly reduces the overhead of enabling Clover, however limited or no reporting is
     * available. The current use of setting this to method is for Test Optimization only.
     *
     * @parameter expression="${maven.clover.instrumentation}"
     */
    private String instrumentation;

    /**
     * The difference (in milliseconds) that a -clover classified artifact can have to a non-clover classified artifact.
     *
     * If the -clover classified artifact is more than cloveredArtifactExpiryInMillis older than the non-clover classified
     * artifact, then the non-classified artifact will be used.
     *
     * This setting defaults to 2000.
     *
     * @parameter expression="${maven.clover.cloveredArtifactExpiryInMillis}" default-value=2000
     */
    private long cloveredArtifactExpiryInMillis;


    /**
     *
     * When creating the clover.jar dependency, what scope to use.
     * This may be one of: compile, test, provided etc. If not specified - provided will be used.
     *
     * @parameter expression="${maven.clover.scope}"
     */
    private String scope;

    /**
     *
     * If set, then the maven-clover2-plugin will not copy files that were excluded, across to the target/clover directory.
     * This is useful if the build is also using plugins such as the maven-gwt-plugin, that scans for resources, and
     * skips a step if none are found. Otherwise, setting this to false could well cause build failures.
     *
     * @parameter expression="${maven.clover.copyExcludedFiles}" default-value="true"
     */
    private boolean copyExcludedFiles = true;

    /**
     * <p>By default, Maven Clover Plugin generates the <code>${java.io.tmpdir}/grover*.jar</code> file during setup,
     * which is next being added as the dependent artifact to the build. As the file has generated, unique
     * name and the jar is not being removed at the end of the build, these files can litter the temporary
     * directory.</p>
     *
     * <p>In case when there is no Groovy code in the project, this parameter can be set to <code>true</code> in order
     * to disable generation of grover.jar artifact.</p>
     *
     * @parameter expression="${maven.clover.skipGroverJar}" default-value="false"
     * @since 3.1.8
     */
    private boolean skipGroverJar = false;

    /**
     * <p>By default, Maven Clover Plugin generates the <code>${java.io.tmpdir}/grover*.jar</code> file during setup,
     * which is next being added as the dependent artifact to the build. As the file has generated, unique
     * name and the jar is not being removed at the end of the build, these files can litter the temporary
     * directory.</p>
     *
     * <p>By setting this parameter you can: <br/>
     * a) specify constant file name for generated artifact, <br/>
     * b) choose location different than ${java.io.tmpdir}.</p>
     *
     * <p>However, you must ensure that: <br/>
     * a) grover.jar will not be deleted till end of the build (for example don't put into ./target directory
     * and next run <code>mvn clover2:setup clean</code>) <br/>
     * b) grover.jar will not be shared among builds with different Maven Clover Plugin versions used (for
     * example if ProjectA uses Clover v 3.1.8 and ProjectB uses Clover v 3.1.9 then they shall have different
     * <code>groverJar</code> locations defined)</p>
     *
     * @parameter expression="${maven.clover.groverJar}"
     * @since 3.1.8
     */
    private File groverJar;

    /**
     * <p>If set to <code>true</code>, Clover will add several properties to the build configuration which
     * disable a build failure for following plugins:
     * <ul>
     *  <li>maven-surefire-plugin (maven.test.failure.ignore=true)</li>
     *  <li>maven-failsafe-plugin (maven.test.failure.ignore=true)</li>
     *  <li>maven-checkstyle-plugin (checkstyle.failOnViolation=false)</li>
     *  <li>maven-pmd-plugin (pmd.failOnViolation=false)</li>
     * </ul></p>
     *
     * <p>Thanks to this, build continues despite test failures or code validation failures and thus
     * it is possible to generate a Clover coverage report for failed tests at the end of the build.</p>
     *
     * <p>Note: before version 3.1.9 the <i>testFailureIgnore</i> property was set to <i>true</i> for
     * the forked Clover lifecycle ('instrument' goal) for 'test' and 'integration-test' phases. Since
     * 3.1.9 it is no longer set.</p>
     *
     * @parameter expression="${maven.clover.setTestFailureIgnore}" default-value="false"
     * @since 3.1.9
     */
    private boolean setTestFailureIgnore = false;

    // HACK: this allows us to reset the source directories to the originals
    private static Map originalSrcMap = new HashMap();
    private static Map originalSrcTestMap = new HashMap();

    public static String getOriginalSrcDir(String module) {
        return (String) originalSrcMap.get(module);
    }

    public static String getOriginalSrcTestDir(String module) {
        return (String) originalSrcTestMap.get(module);
    }

    /**
     * {@inheritDoc}
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        if (skip) {
            getLog().info("Skipping clover instrumentation.");
            return;
        }

        configureTestFailureIgnore();
        resetSrcDirsOriginal(getProject().getArtifact(), this);

        final File outDir = new File(this.cloverOutputDirectory, getSrcName());
        String cloverOutputSourceDirectory = outDir.getPath();
        String cloverOutputTestSourceDirectory = new File( this.cloverOutputDirectory, getSrcTestName()).getPath();
        new File( resolveCloverDatabase() ).getParentFile().mkdirs();

        super.execute();

        logArtifacts( "before changes" );

        // Instrument both the main sources and the test sources if the user has configured it
        final MainInstrumenter mainInstrumenter = new MainInstrumenter( this, cloverOutputSourceDirectory );
        final TestInstrumenter testInstrumenter = new TestInstrumenter( this, cloverOutputTestSourceDirectory );

        if ( isJavaProject() )
        {
            mainInstrumenter.instrument();
            if ( this.includesTestSourceRoots )
            {
                testInstrumenter.instrument();
            }
        }

        addCloverDependencyToCompileClasspath();
        injectGrover(outDir);

        swizzleCloverDependencies();
        // Modify Maven model so that it points to the new source directories and to the clovered
        // artifacts instead of the original values.
        String originalSrcDir = mainInstrumenter.redirectSourceDirectories();
        originalSrcMap.put(getProject().getId(), originalSrcDir);
        if ( this.includesTestSourceRoots )
        {
            String originalSrcTestDir = testInstrumenter.redirectSourceDirectories();
            originalSrcTestMap.put(getProject().getId(), originalSrcTestDir);
        }
        redirectOutputDirectories();
        redirectArtifact();

        logArtifacts( "after changes" );
    }

    /**
     * Sets several properties related with test failures for Surefire, Failsafe, PMD and Checkstyle plugins.
     * Thanks to this, the build in default or forked lifecycle can continue and we can generate Clover report
     * even in presence of test failures.
     */
    private void configureTestFailureIgnore() {
        if (setTestFailureIgnore) {
            getLog().debug("Configuring testFailureIgnore=true and failOnViolation=false");
            final Properties properties = getProject().getProperties();
            properties.put("maven.test.failure.ignore", "true");  // surefire and failsafe
            properties.put("checkstyle.failOnViolation", "false");
            properties.put("pmd.failOnViolation", "false");
        }
    }

    private void injectGrover(File outDir)
    {
        if (skipGroverJar) {
            getLog().info("Generation of Clover Groovy configuration is disabled. No Groovy instrumentation will occur.");
            return;
        }

        // create the groovy config for Clover's ASTTransformer
        InstrumentationConfig config = new InstrumentationConfig();
        config.setProjectName(this.getProject().getName());
        config.setInitstring(this.resolveCloverDatabase());
        config.setTmpDir(outDir);

        final List includeFiles = calcIncludedFiles();
        getLog().debug("Clover including the following files for Groovy instrumentation: " + includeFiles);
        config.setIncludedFiles(includeFiles);
        config.setEnabled(true);
        config.setEncoding(getEncoding());
        //Don't pass in an instance of DistributedCoverage because it can't be deserialised
        //by Grover (ClassNotFoundException within the groovyc compiler)
        config.setDistributedConfig(getDistributedCoverage() == null ? null : new DistributedConfig(getDistributedCoverage().getConfigString()));


        try
        {
            File groverJar = GroovycSupport.extractGroverJar(this.groverJar, false);
            File groverConfigDir = GroovycSupport.newConfigDir(config, new File(getProject().getBuild().getOutputDirectory()));
            final Resource groverConfigResource = new Resource();
            groverConfigResource.setDirectory(groverConfigDir.getPath());
            getProject().addResource(groverConfigResource);

            // get the clover artifact, and use the same version number for grover...
            Artifact cloverArtifact = findCloverArtifact(this.pluginArtifacts);
            // add grover to the compilation classpath
            final Artifact groverArtifact = artifactFactory.createBuildArtifact(cloverArtifact.getGroupId(), "grover", cloverArtifact.getVersion(), "jar");
            groverArtifact.setFile(groverJar);
            groverArtifact.setScope(Artifact.SCOPE_SYSTEM);
            addArtifactDependency(groverArtifact);
        }
        catch (IOException e)
        {
            getLog().error("Could not create Clover Groovy configuration file. No Groovy instrumentation will occur. " + e.getMessage(), e);
        }
    }

    private List calcIncludedFiles()
    {
        GroovySourceScanner gScanner = new GroovySourceScanner(this, getProject().getBuild().getOutputDirectory());
        GroovyTestScanner gTestScanner = new GroovyTestScanner(this, getProject().getBuild().getOutputDirectory());

        List sources =  extractIncludes(gScanner.getSourceFilesToInstrument());
        List tests =  extractIncludes(gTestScanner.getSourceFilesToInstrument());
        List allSource = new ArrayList(sources);
        allSource.addAll(tests);
        return allSource;
    }

    private ArrayList extractIncludes(Map srcFiles)
    {
        ArrayList includeFiles = new ArrayList();
        for (Iterator iterator = srcFiles.keySet().iterator(); iterator.hasNext();)
        {
            final String dirName = (String) iterator.next();
            final String[] includes = (String[]) srcFiles.get(dirName);
            for (int i = 0; i < includes.length; i++)
            {
                includeFiles.add(new File(dirName, includes[i]));
            }
        }
        return includeFiles;
    }

    public static void resetSrcDirsOriginal(Artifact artifact, CompilerConfiguration config) {
        if (originalSrcMap.containsKey(artifact.getId())) {
            final String sourceDirectory = (String) originalSrcMap.get(artifact.getId());
            MainInstrumenter mainInstrumenter = new MainInstrumenter(config, sourceDirectory);
            mainInstrumenter.redirectSourceDirectories();

        }
        if (originalSrcTestMap.containsKey(artifact.getId())) {
            final String testDirectory = (String) originalSrcTestMap.get(artifact.getId());
            TestInstrumenter instrumenter = new TestInstrumenter(config, testDirectory);
            instrumenter.redirectSourceDirectories();
        }
    }

    protected String getSrcTestName() {
        return "src-test";
    }

    protected String getSrcName() {
        return "src";
    }

    private boolean isJavaProject()
    {
        ArtifactHandler artifactHandler = getProject().getArtifact().getArtifactHandler();

        if ( !"java".equals( artifactHandler.getLanguage() ) )
        {
            getLog().warn( "The reported language of this project is " + artifactHandler.getLanguage() + ", attempting to instrument sources anyway.");
        }
        return true;
    }

    protected void redirectOutputDirectories()
    {
        // Explicitely set the output directory to be the Clover one so that all other plugins executing
        // thereafter output files in the Clover output directory and not in the main output directory.
        getProject().getBuild().setDirectory( this.cloverOutputDirectory );

        // TODO: Ugly hack below. Changing the directory should be enough for changing the values of all other
        // properties depending on it!
        getProject().getBuild().setOutputDirectory( new File( this.cloverOutputDirectory, "classes" ).getPath() );
        getProject().getBuild().setTestOutputDirectory(new File( this.cloverOutputDirectory, "test-classes" ).getPath() );
    }

    /**
     * Modify main artifact to add a "clover" classifier to it so that it's not mixed with the main artifact of
     * a normal build.
     */
    protected void redirectArtifact()
    {
        // Only redirect main artifact for non-pom projects
        if ( !getProject().getPackaging().equals( "pom" ) )
        {
            Artifact oldArtifact = getProject().getArtifact();
            Artifact newArtifact = this.artifactFactory.createArtifactWithClassifier( oldArtifact.getGroupId(),
                oldArtifact.getArtifactId(), oldArtifact.getVersion(), oldArtifact.getType(), "clover" );
            getProject().setArtifact( newArtifact );

            final String finalName =
                    getProject().getBuild().getFinalName() == null ?
                    (getProject().getArtifactId() + "-" + getProject().getVersion()) 
                    : getProject().getBuild().getFinalName();
            
            getProject().getBuild().setFinalName(finalName + (useCloverClassifier ? "-clover" : ""));
        }
    }

    /**
     * Browse through all project dependencies and try to find a clovered version of the dependency. If found
     * replace the main depedencency by the clovered version.
     */
    private void swizzleCloverDependencies()
    {
        final Set swizzledDependencyArtifacts = swizzleCloverDependencies(getProject().getDependencyArtifacts());


        // only swizzle the difference between artifacts and dependency artifacts to ensure no dupes
        final Set artifacts = getProject().getArtifacts();
        final Set dependencyArtifacts = getProject().getDependencyArtifacts();
        artifacts.removeAll(dependencyArtifacts);

        final Set swizzledArtifacts = swizzleCloverDependencies(artifacts);
        swizzledArtifacts.addAll(swizzledDependencyArtifacts);

        getProject().setDependencyArtifacts(swizzledDependencyArtifacts);
        getProject().setArtifacts(swizzledArtifacts);
    }

    protected Set swizzleCloverDependencies( Set artifacts )
    {
        Set resolvedArtifacts = new LinkedHashSet();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            // Do not try to find Clovered versions for artifacts with classifiers. This is because Maven2 only
            // supports a single classifier per artifact and thus if we replace the original classifier with
            // a Clover classifier the artifact will fail to perform properly as intended originally. This is a
            // limitation.
            if ( artifact.getClassifier() == null )
            {
                Artifact cloveredArtifact = this.artifactFactory.createArtifactWithClassifier( artifact.getGroupId(),
                    artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), "clover" );

                // Try to resolve the artifact with a clover classifier. If it doesn't exist, simply add the original
                // artifact. If found, use the clovered artifact.
                try
                {
                    this.artifactResolver.resolve( cloveredArtifact, new ArrayList(), localRepository );

                    // Set the same scope as the main artifact as this is not set by createArtifactWithClassifier.
                    cloveredArtifact.setScope( artifact.getScope() );

                    // Check the timestamp of the artifact. If the found clovered version is older than the
                    // non-clovered one we need to use the non-clovered version. This is to handle use case such as:
                    // - Say you have a module B that depends on a module A
                    // - You run Clover on A
                    // - You make modifications on A such that B would fail if not built with the latest version of A
                    // - You try to run the Clover plugin on B. The build would fail if we didn't pick the latest
                    //   version between the original A version and the clovered version.
                    //
                    // We provide a 'fudge-factor' of 2 seconds, as the clover artifact is created first.
                    if ( cloveredArtifact.getFile().lastModified() + cloveredArtifactExpiryInMillis < artifact.getFile().lastModified() )
                    {
                        getLog().warn( "Using [" + artifact.getId() + "], built on " + new Date(artifact.getFile().lastModified()) +
                                " even though a Clovered version exists "
                            + "but it's older (lastModified: " + new Date(cloveredArtifact.getFile().lastModified())
                                + " ) and could fail the build. Please consider running Clover again on that "
                            + "dependency's project." );
                        resolvedArtifacts.add( artifact );

                    }
                    else
                    {
                        resolvedArtifacts.add( cloveredArtifact );
                    }
                }
                catch ( ArtifactResolutionException e )
                {
                    getLog().warn( "Skipped dependency [" + artifact.getId() + "] due to resolution error: " + e.getMessage() );
                    resolvedArtifacts.add( artifact );
                }
                catch ( ArtifactNotFoundException e )
                {
                    getLog().debug( "Skipped dependency [" + artifact.getId() + "] as the clovered artifact could not be found" );
                    resolvedArtifacts.add( artifact );
                }
            }
            else
            {
                getLog().debug( "Skipped dependency [" + artifact.getId() + "] as it has a classifier" );
                resolvedArtifacts.add( artifact );
            }
        }

        return resolvedArtifacts;
    }

    protected Artifact findCloverArtifact( List pluginArtifacts )
    {
        Artifact cloverArtifact = null;
        Iterator artifacts = pluginArtifacts.iterator();
        while ( artifacts.hasNext() && cloverArtifact == null )
        {
            Artifact artifact = (Artifact) artifacts.next();

            // We identify the clover JAR by checking the groupId and artifactId.
            if ( "com.cenqua.clover".equals( artifact.getGroupId() )
                && "clover".equals( artifact.getArtifactId() ) )
            {
                cloverArtifact = artifact;
            }
        }
        return cloverArtifact;
    }

    private void addCloverDependencyToCompileClasspath()
        throws MojoExecutionException
    {
        Artifact cloverArtifact = findCloverArtifact( this.pluginArtifacts );
        if ( cloverArtifact == null )
        {
            throw new MojoExecutionException(
                "Couldn't find [com.cenqua.clover:clover] artifact in plugin dependencies" );
        }

        final String jarScope = scope == null ? Artifact.SCOPE_PROVIDED : scope;
        cloverArtifact = artifactFactory.createArtifact( cloverArtifact.getGroupId(), cloverArtifact.getArtifactId(),
            cloverArtifact.getVersion(), jarScope, cloverArtifact.getType() );
        try
        {
            this.artifactResolver.resolve( cloverArtifact, repositories, localRepository );
        }
        catch (AbstractArtifactResolutionException e)
        {
            throw new MojoExecutionException("Could not resolve the clover artifact ( " +
                                                cloverArtifact.getId() +
                                                " ) in the localRepository: " + localRepository.getUrl(), e);
        }

        addArtifactDependency(cloverArtifact);
    }

    private void addArtifactDependency(Artifact cloverArtifact)
    {
        // TODO: use addArtifacts when it's implemented, see http://jira.codehaus.org/browse/MNG-2197
        Set set = new LinkedHashSet( getProject().getDependencyArtifacts() );
        set.add( cloverArtifact );
        getProject().setDependencyArtifacts( set );
    }

    private void logArtifacts( String message )
    {
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "[Clover] List of dependency artifacts " + message + ":" );
            logArtifacts( getProject().getDependencyArtifacts() );

            getLog().debug( "[Clover] List of artifacts " + message + ":" );
            logArtifacts( getProject().getArtifacts() );
        }
    }

    private void logArtifacts( Set artifacts )
    {
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            getLog().debug( "[Clover]   Artifact [" + artifact.getId() + "], scope = [" + artifact.getScope() + "]" );
        }
    }

    protected void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    protected void setArtifactResolver( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = artifactResolver;
    }

    public Set getIncludes()
    {
        if (includesList == null) {
            return this.includes;
        }
        if (includesList != null) {
            return new HashSet(Arrays.asList(includesList.split(",")));
        }
        return this.includes;
    }

    public Set getExcludes()
    {
        if (excludesList == null) {
            return this.excludes;
        }
        if (excludesList != null) {
            this.excludes.addAll(Arrays.asList(excludesList.split(",")));
        }
        return this.excludes;
    }

    public boolean includesAllSourceRoots()
    {
        return this.includesAllSourceRoots;
    }

    public boolean isUseFullyQualifiedJavaLang() {
        return useFullyQualifiedJavaLang;
    }

    public String getEncoding() {
        return encoding;
    }

    public Map getMethodContexts() {
        return methodContexts;
    }

    public Map getStatementContexts() {
        return statementContexts;
    }

    public int getStaleMillis() {
        return staleMillis;
    }

    public String getInstrumentation() {
        return instrumentation;
    }

    public boolean copyExcludedFiles() {
        return copyExcludedFiles;
    }

    public DistributedCoverage getDistributedCoverage() {
        return distributedCoverage;
    }
}
