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
import com.atlassian.clover.cfg.instr.InstrumentationConfig;
import com.atlassian.clover.remote.DistributedConfig;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverInstrumentMojo;
import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;
import com.atlassian.maven.plugin.clover.internal.instrumentation.MainInstrumenter;
import com.atlassian.maven.plugin.clover.internal.instrumentation.TestInstrumenter;
import com.atlassian.maven.plugin.clover.internal.scanner.LanguageFileExtensionFilter;
import com.atlassian.maven.plugin.clover.internal.scanner.MainSourceScanner;
import com.atlassian.maven.plugin.clover.internal.scanner.TestSourceScanner;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/*
 * TRICKY PART HOW JAVA AND GROOVY SOURCE FOLDERS ARE HANDLED
 *
 * PROBLEM DIMENSIONS:
 *  #1 language:
 *      -> java only
 *      -> java + groovy
 *      -> groovy only
 *  #2 source folders:
 *      -> src/xxx/java
 *      -> src/xxx/groovy
 *      -> generated-sources/xxx
 *  #3 location of java source files:
 *      -> in src/xxx/java
 *      -> in src/xxx/groovy - NOT SUPPORTED BY CLOVER (see reasons below)
 *  #4 location of groovy source files:
 *      -> src/xxx/java
 *      -> src/xxx/groovy
 *  #5 definition of groovy source folders
 *      -> by <sourceDirectory>, <testSourceDirectory> parameters in POM
 *      -> by add-source, add-test-source goals in build-helper-maven-plugin
 *      -> by extensions=true option in groovy-eclipse-compiler
 *      -> not defined at all (but src/xxx/groovy location is then internally added by gmaven or groovy-eclipse-plugin)
 *  #6 groovy plugins
 *      -> gmaven
 *      -> groovy-eclipse-plugin
 *  #7 instrumentation scope
 *      -> non-generated sources
 *      -> all sources
 *  #8 java/groovy compilation
 *      -> separated (maven-compiler compiles java, gmaven compiles groovy)
 *      -> joint compilation (groovy-eclipse-plugin compiles both java and groovy)
 *  #9 build lifecycle
 *      -> non-forked (clover:setup)
 *      -> forked (clover:instrument)
 *
 * =====================================================================================================================
 * 1) includeAllSourceRoots = false, java+groovy in 'src/(main|test)/java'
 *
 *  -> source directories are read from getCompileSourceRoots()
 *  -> *.java files are instrumented in the source and saved in 'src-(test-)instrumented'
 *  -> *.groovy are copied "as is" to 'src-(test-)instrumented' too
 *  -> source roots are overridden:
 *          'src/main/java' -> 'src-instrumented'
 *          'src/test/java' -> 'src-test-instrumented'
 *  -> groovyc compiles sources from source roots listed above:
 *       -> *.java files are already instrumented in the source, compile "as is"
 *       -> *.groovy files are being instrumented in the AST
 *
 * =====================================================================================================================
 * 1b) includeAllSourceRoots = true, java+groovy in 'src/(main|test)/java', generated sources in
 * 'target/generated-(test-)sources'
 *
 *   -> as for case 1) but 'generated-(test-)sources' are instrumented too and stored in 'src-(test-)instrumented'
 *
 * =====================================================================================================================
 * 2) includeAllSourceRoots = false, java in 'src/(main|test)/java', groovy in 'src/(main|test)/(java|groovy)'
 *
 *  -> build-helper-maven-plugin adds 'src/(main|test)/groovy' as extra source root
 *  -> source directories are read from getCompileSourceRoots()
 *  -> *.java files are instrumented in the source and saved in 'src-(test-)instrumented'
 *  -> *.groovy files
 *          from 'src/(main|test)/java' are copied to 'src-(test-)instrumented'
 *          from 'src/(main|test)/groovy' are not copied anywhere
 *  -> source roots are switched:
 *          'src/main/java' -> 'src-instrumented'
 *          'src/test/java' -> 'src-test-instrumented'
 *          'src/main/groovy' -> (unchanged)
 *          'src/test/groovy' -> (unchanged)
 *  -> groovyc compiles sources from all source roots listed above
 *       -> *.java files are already instrumented in the source, compile "as is"
 *       -> *.groovy files are instrumented in the AST
 *
 * =====================================================================================================================
 * 2b) includeAllSourceRoots = true, java in 'src/(main|test)/java', groovy in 'src/(main|test)/(java|groovy)',
 * generated sources in 'target/generated-(test-)sources
 *
 *   -> as for case 2) but 'generated-(test-)sources' are instrumented too and stored in 'src-(test-)instrumented'
 *
 * =====================================================================================================================
 * 3) includeAllSourceRoots = false, java in 'src/(main|test)/java', groovy in 'src/(main|test)/(java|groovy)'
 *
 *  -> the 'src/(main|test)/groovy' is NOT added as extra source root
 *      -> so that getCompileSourceRoots() does not return it in the list
 *      -> groovy-eclipse-plugin will add the 'src/(main|test)/groovy' location internally (typically after the
 *         clover:setup goal is finished)
 *  -> source directories are read from getCompileSourceRoots()
 *      -> GroovyMain/TestSourceScanner contains additional hardcoded location for 'src/(main|test)/groovy'
 *  -> *.java files are instrumented in the source and saved in 'src-(test-)instrumented'
 *  -> *.groovy files
 *          from 'src/(main|test)/java' are copied to 'src-(test-)instrumented'
 *          from 'src/(main|test)/groovy' are not copied anywhere
 *  -> source roots are switched:
 *          'src/main/java' -> 'src-instrumented'
 *          'src/test/java' -> 'src-test-instrumented'
 *   -> groovyc compiles sources from all source roots listed above
 *       -> *.java files are already instrumented, compile "as is"
 *       -> *.groovy files are instrumented in the AST
 *       -> groovy-eclipse-plugin internally adds 'src/(main|test)/groovy' to the list of source roots
 *
 * =====================================================================================================================
 * 3b) includeAllSourceRoots = true, java in 'src/(main|test)/java', groovy in 'src/(main|test)/(java|groovy)'
 * generated sources in 'target/generated-(test-)sources
 *
 *   -> as for case 3) but 'generated-(test-)sources' are instrumented too and stored in 'src-(test-)instrumented'
 *
 * =====================================================================================================================
 * 4) includeAllSourceRoots = false, no java code, groovy in 'src/(main|test)/groovy',
 * sourceDirectory/testSourceDirectory are used on POM
 *
 *   -> the 'src/(main|test)/groovy' is added as sourceDirectory and testSourceDirectory
 *      -> so that getProject().getBuild().getSourceDirectory() returns it
 *      -> so that getProject().getCompileSourceRoots() returns it
 *   -> *.groovy files are not copied anywhere
 *   -> source roots are NOT switched
 *   -> groovyc compiles sources from original source roots ('src/(main|test)/groovy')
 *
 * =====================================================================================================================
 * 4b) includeAllSourceRoots = true, no java code, groovy in 'src/(main|test)/groovy',
 * sourceDirectory/testSourceDirectory are used on POM
 *
 *   -> as for case 4) but 'generated-(test-)sources' are passed to groovyc
 *
 * =====================================================================================================================
 * 5) includeAllSourceRoots = false, java in src/xxx/java, no groovy code, no groovy plugin
 *
 *   -> standard behaviour, 'src/(main|test)/java' is instrumented into 'target/clover/src-(test-)instrumented'
 *   and compiled
 *
 * =====================================================================================================================
 * 5b) includeAllSourceRoots = true, java in src/xxx/java, no groovy code, no groovy plugin
 *
 *   -> standard behaviour, 'src/(main|test)/java' as well as 'target/generated-(test-)sources' are instrumented
 *   into 'target/clover/src-(test-)instrumented' and compiled
 */


/**
 * <p>Instrument source roots.</p>
 * <p><b>Note 1: Do not call this MOJO directly. It is meant to be called in a custom forked lifecycle by the other
 * Clover plugin MOJOs.</b></p>
 * <p><b>Note 2: We bind this mojo to the "validate" phase so that it executes prior to any other mojos</b></p>
 *
 * @goal instrumentInternal
 * @phase validate
 * @requiresDependencyResolution test
 */
public class CloverInstrumentInternalMojo extends AbstractCloverInstrumentMojo {

    public static final String CLOVER_CORE_GROUP_ID = "com.atlassian.clover";
    public static final String CLOVER_CORE_ARTIFACT_ID = "clover";

    /**
     * <p>List of all artifacts for this Clover plugin provided by Maven. This is used internally to get a handle on
     * the Clover JAR artifact.</p>
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     */
    private List<Artifact> pluginArtifacts;

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
    protected List<ArtifactRepository> repositories;

    // HACK: this allows us to reset the source directories to the originals
    private static Map<String, String> originalSrcMap = new HashMap<String, String>();
    private static Map<String, String> originalSrcTestMap = new HashMap<String, String>();

    public static String getOriginalSrcDir(final String module) {
        return originalSrcMap.get(module);
    }

    public static String getOriginalSrcTestDir(final String module) {
        return originalSrcTestMap.get(module);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping clover instrumentation.");
            return;
        }

        super.execute();

        configureTestFailureIgnore();
        resetSrcDirsOriginal(getProject().getArtifact(), this);

        final File outDir = new File(this.cloverOutputDirectory, getSrcName());
        final String cloverOutputSourceDirectory = outDir.getPath();
        final String cloverOutputTestSourceDirectory = new File(this.cloverOutputDirectory, getSrcTestName()).getPath();
        new File(resolveCloverDatabase()).getParentFile().mkdirs();

        logArtifacts("before changes");

        // Instrument both the main sources and the test sources if the user has configured it
        final MainInstrumenter mainInstrumenter = new MainInstrumenter(this, cloverOutputSourceDirectory);
        final TestInstrumenter testInstrumenter = new TestInstrumenter(this, cloverOutputTestSourceDirectory);

        if (isJavaProject()) {
            mainInstrumenter.instrument();
            if (this.includesTestSourceRoots) {
                testInstrumenter.instrument();
            }
        }

        // add clover.jar to classpath
        addCloverDependencyToCompileClasspath();

        // deal with '-clover' artifacts in dependencies
        swizzleCloverDependencies();

        // Modify Maven model so that it points to the new source directories and to the clovered
        // artifacts instead of the original values.
        final String originalSrcDir = mainInstrumenter.redirectSourceDirectories();
        originalSrcMap.put(getProject().getArtifact().getId(), originalSrcDir);
        if (this.includesTestSourceRoots) {
            final String originalSrcTestDir = testInstrumenter.redirectSourceDirectories();
            originalSrcTestMap.put(getProject().getArtifact().getId(), originalSrcTestDir);
        }

        // add instrumentation of groovy sources
        injectGrover(outDir);

        redirectOutputDirectories();
        redirectArtifact();

        logArtifacts("after changes");
    }

    @Override
    protected boolean shouldRedirectArtifacts() {
        return true;
    }

    @Override
    protected boolean shouldRedirectOutputDirectories() {
        return true;
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

    /**
     * @param outDir - output directory for temporary artifacts
     */
    private void injectGrover(final File outDir) {
        if (skipGroverJar) {
            getLog().info("Generation of Clover Groovy configuration is disabled. No Groovy instrumentation will occur.");
            return;
        }

        // create the groovy config for Clover's ASTTransformer
        InstrumentationConfig config = new InstrumentationConfig();
        config.setProjectName(this.getProject().getName());
        config.setInitstring(this.resolveCloverDatabase());
        config.setTmpDir(outDir);

        final List<File> includeFiles = calcIncludedFilesForGroovy();
        getLog().debug("Clover including the following files for Groovy instrumentation: " + includeFiles);
        config.setIncludedFiles(includeFiles);
        config.setEnabled(true);
        config.setEncoding(getEncoding());
        //Don't pass in an instance of DistributedCoverage because it can't be deserialised
        //by Grover (ClassNotFoundException within the groovyc compiler)
        config.setDistributedConfig(getDistributedCoverage() == null ? null : new DistributedConfig(getDistributedCoverage().getConfigString()));


        try {
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
        } catch (IOException e) {
            getLog().error("Could not create Clover Groovy configuration file. No Groovy instrumentation will occur. " + e.getMessage(), e);
        }
    }

    /**
     * @return List&lt;File&gt;
     * @see com.atlassian.maven.plugin.clover.internal.instrumentation.AbstractInstrumenter#instrument()
     * @see #redirectOutputDirectories()
     * @see <a href="http://groovy.codehaus.org/Groovy-Eclipse+compiler+plugin+for+Maven">Groovy-Eclipse+compiler+plugin+for+Maven</a>
     */
    protected List<File> calcIncludedFilesForGroovy() {
        final MainSourceScanner groovyMainScanner = new MainSourceScanner(this, getProject().getBuild().getOutputDirectory());
        final List<File> mainGroovyFiles = extractIncludes(
                groovyMainScanner.getSourceFilesToInstrument(LanguageFileExtensionFilter.GROOVY_LANGUAGE, false));
        final TestSourceScanner groovyTestScanner = new TestSourceScanner(this, getProject().getBuild().getOutputDirectory());
        final List<File> testGroovyFiles = extractIncludes(
                groovyTestScanner.getSourceFilesToInstrument(LanguageFileExtensionFilter.GROOVY_LANGUAGE, false));

        // combine lists
        final List<File> allSources = new ArrayList<File>(mainGroovyFiles);
        allSources.addAll(testGroovyFiles);
        return allSources;
    }

    private ArrayList<File> extractIncludes(final Map<String, String[]> srcFiles) {
        final ArrayList<File> includeFiles = new ArrayList<File>();
        for (final String dirName : srcFiles.keySet()) {
            final String[] includes = srcFiles.get(dirName);
            for (final String include : includes) {
                includeFiles.add(new File(dirName, include));
            }
        }
        return includeFiles;
    }

    public static void resetSrcDirsOriginal(final Artifact artifact, final CompilerConfiguration config) {
        if (originalSrcMap.containsKey(artifact.getId())) {
            final String sourceDirectory = originalSrcMap.get(artifact.getId());
            MainInstrumenter mainInstrumenter = new MainInstrumenter(config, sourceDirectory);
            mainInstrumenter.redirectSourceDirectories();

        }
        if (originalSrcTestMap.containsKey(artifact.getId())) {
            final String testDirectory = originalSrcTestMap.get(artifact.getId());
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

    private boolean isJavaProject() {
        final ArtifactHandler artifactHandler = getProject().getArtifact().getArtifactHandler();

        if (!"java".equals(artifactHandler.getLanguage())) {
            getLog().debug("The reported language of this project is " + artifactHandler.getLanguage() + ", attempting to instrument sources anyway.");
        }
        return true;
    }

    protected void redirectOutputDirectories() {
        if (shouldRedirectOutputDirectories()) {
            // Explicitly set the output directory to be the Clover one so that all other plugins executing
            // thereafter output files in the Clover output directory and not in the main output directory.
            getProject().getBuild().setDirectory(this.cloverOutputDirectory);

            // TODO: Ugly hack below. Changing the directory should be enough for changing the values of all other
            // properties depending on it!
            getProject().getBuild().setOutputDirectory(new File(this.cloverOutputDirectory, "classes").getPath());
            getProject().getBuild().setTestOutputDirectory(new File(this.cloverOutputDirectory, "test-classes").getPath());
        }
    }

    /**
     * Modify main artifact to add a "clover" classifier to it so that it's not mixed with the main artifact of
     * a normal build.
     */
    protected void redirectArtifact() {
        if (shouldRedirectArtifacts()) {
            // Only redirect main artifact for non-pom projects
            if (!getProject().getPackaging().equals("pom")) {
                Artifact oldArtifact = getProject().getArtifact();
                Artifact newArtifact = this.artifactFactory.createArtifactWithClassifier(oldArtifact.getGroupId(),
                        oldArtifact.getArtifactId(), oldArtifact.getVersion(), oldArtifact.getType(), "clover");
                getProject().setArtifact(newArtifact);

                final String finalName =
                        getProject().getBuild().getFinalName() == null ?
                                (getProject().getArtifactId() + "-" + getProject().getVersion())
                                : getProject().getBuild().getFinalName();

                getProject().getBuild().setFinalName(finalName + (useCloverClassifier ? "-clover" : ""));
            }
        }
    }

    /**
     * Browse through all project dependencies and try to find a clovered version of the dependency. If found
     * replace the main depedencency by the clovered version.
     */
    private void swizzleCloverDependencies() {
        final Set<Artifact> swizzledDependencyArtifacts = swizzleCloverDependencies(getProject().getDependencyArtifacts());

        // only swizzle the difference between artifacts and dependency artifacts to ensure no dupes
        final Set<Artifact> artifacts = getProject().getArtifacts();
        final Set<Artifact> dependencyArtifacts = getProject().getDependencyArtifacts();
        artifacts.removeAll(dependencyArtifacts);

        final Set<Artifact> swizzledArtifacts = swizzleCloverDependencies(artifacts);
        swizzledArtifacts.addAll(swizzledDependencyArtifacts);

        getProject().setDependencyArtifacts(swizzledDependencyArtifacts);
        getProject().setArtifacts(swizzledArtifacts);
    }

    protected Set<Artifact> swizzleCloverDependencies(final Set<Artifact> artifacts) {
        Set<Artifact> resolvedArtifacts = new LinkedHashSet<Artifact>();
        for (Artifact artifact : artifacts) {
            // Do not try to find Clovered versions for artifacts with classifiers. This is because Maven only
            // supports a single classifier per artifact and thus if we replace the original classifier with
            // a Clover classifier the artifact will fail to perform properly as intended originally. This is a
            // limitation.
            if (artifact.getClassifier() == null) {
                Artifact cloveredArtifact = this.artifactFactory.createArtifactWithClassifier(artifact.getGroupId(),
                        artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), "clover");

                // Try to resolve the artifact with a clover classifier. If it doesn't exist, simply add the original
                // artifact. If found, use the clovered artifact.
                try {
                    this.artifactResolver.resolve(cloveredArtifact, new ArrayList(), localRepository);

                    // Set the same scope as the main artifact as this is not set by createArtifactWithClassifier.
                    cloveredArtifact.setScope(artifact.getScope());

                    // Check the timestamp of the artifact. If the found clovered version is older than the
                    // non-clovered one we need to use the non-clovered version. This is to handle use case such as:
                    // - Say you have a module B that depends on a module A
                    // - You run Clover on A
                    // - You make modifications on A such that B would fail if not built with the latest version of A
                    // - You try to run the Clover plugin on B. The build would fail if we didn't pick the latest
                    //   version between the original A version and the clovered version.
                    //
                    // We provide a 'fudge-factor' of 2 seconds, as the clover artifact is created first.
                    if (cloveredArtifact.getFile().lastModified() + cloveredArtifactExpiryInMillis < artifact.getFile().lastModified()) {
                        getLog().warn("Using [" + artifact.getId() + "], built on " + new Date(artifact.getFile().lastModified()) +
                                " even though a Clovered version exists "
                                + "but it's older (lastModified: " + new Date(cloveredArtifact.getFile().lastModified())
                                + " ) and could fail the build. Please consider running Clover again on that "
                                + "dependency's project.");
                        resolvedArtifacts.add(artifact);

                    } else {
                        resolvedArtifacts.add(cloveredArtifact);
                    }
                } catch (ArtifactResolutionException e) {
                    getLog().warn("Skipped dependency [" + artifact.getId() + "] due to resolution error: " + e.getMessage());
                    resolvedArtifacts.add(artifact);
                } catch (ArtifactNotFoundException e) {
                    getLog().debug("Skipped dependency [" + artifact.getId() + "] as the clovered artifact could not be found");
                    resolvedArtifacts.add(artifact);
                }
            } else {
                getLog().debug("Skipped dependency [" + artifact.getId() + "] as it has a classifier");
                resolvedArtifacts.add(artifact);
            }
        }

        return resolvedArtifacts;
    }

    protected Artifact findCloverArtifact(final List<Artifact> pluginArtifacts) {
        Artifact cloverArtifact = null;
        Iterator<Artifact> artifactsIterator = pluginArtifacts.iterator();
        while (artifactsIterator.hasNext() && cloverArtifact == null) {
            Artifact artifact = artifactsIterator.next();

            // We identify the clover JAR by checking the groupId and artifactId.
            if (CLOVER_CORE_GROUP_ID.equals(artifact.getGroupId())
                    && CLOVER_CORE_ARTIFACT_ID.equals(artifact.getArtifactId())) {
                cloverArtifact = artifact;
            }
        }
        return cloverArtifact;
    }

    private void addCloverDependencyToCompileClasspath()
            throws MojoExecutionException {
        Artifact cloverArtifact = findCloverArtifact(this.pluginArtifacts);
        if (cloverArtifact == null) {
            throw new MojoExecutionException(
                    "Couldn't find [" + CLOVER_CORE_GROUP_ID + ":" + CLOVER_CORE_ARTIFACT_ID + "] artifact in plugin dependencies");
        }

        final String jarScope = scope == null ? Artifact.SCOPE_PROVIDED : scope;
        cloverArtifact = artifactFactory.createArtifact(cloverArtifact.getGroupId(), cloverArtifact.getArtifactId(),
                cloverArtifact.getVersion(), jarScope, cloverArtifact.getType());
        try {
            this.artifactResolver.resolve(cloverArtifact, repositories, localRepository);
        } catch (AbstractArtifactResolutionException e) {
            throw new MojoExecutionException("Could not resolve the clover artifact ( " +
                    cloverArtifact.getId() +
                    " ) in the localRepository: " + localRepository.getUrl(), e);
        }

        addArtifactDependency(cloverArtifact);
    }

    private void addArtifactDependency(final Artifact cloverArtifact) {
        // TODO: use addArtifacts when it's implemented, see http://jira.codehaus.org/browse/MNG-2197
        Set<Artifact> set = new LinkedHashSet<Artifact>(getProject().getDependencyArtifacts());
        set.add(cloverArtifact);
        getProject().setDependencyArtifacts(set);
    }

    private void logArtifacts(final String message) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("[Clover] List of dependency artifacts " + message + ":");
            logArtifacts(getProject().getDependencyArtifacts());

            getLog().debug("[Clover] List of artifacts " + message + ":");
            logArtifacts(getProject().getArtifacts());
        }
    }

    private void logArtifacts(final Set<Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            getLog().debug("[Clover]   Artifact [" + artifact.getId() + "], scope = [" + artifact.getScope() + "]");
        }
    }

    protected void setArtifactFactory(final ArtifactFactory artifactFactory) {
        this.artifactFactory = artifactFactory;
    }

    protected void setArtifactResolver(final ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

}
