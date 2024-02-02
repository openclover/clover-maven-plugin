package com.atlassian.maven.plugin.clover.internal;

import clover.org.apache.commons.lang3.StringUtils;
import com.atlassian.clover.util.IOStreamUtils;
import com.atlassian.maven.plugin.clover.DistributedCoverage;
import com.atlassian.maven.plugin.clover.MethodWithMetricsContext;
import com.atlassian.maven.plugin.clover.TestSources;
import com.atlassian.maven.plugin.clover.internal.lifecycle.BuildLifecycleAnalyzer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Common settings for clover:instr / clover:setup MOJOs.
 */
public abstract class AbstractCloverInstrumentMojo extends AbstractCloverMojo implements CompilerConfiguration {

    /**
     * <p>The difference (in milliseconds) that a -clover classified artifact can have to a non-clover classified artifact.</p>
     * <p>If the -clover classified artifact is more than cloveredArtifactExpiryInMillis older than the non-clover classified
     * artifact, then the non-classified artifact will be used.</p>
     * <p>This setting defaults to 2000.</p>
     */
    @Parameter(property = "maven.clover.cloveredArtifactExpiryInMillis", defaultValue = "2000")
    protected long cloveredArtifactExpiryInMillis;

    /**
     * If set, then the clover-maven-plugin will not copy files that were excluded, across to the target/clover directory.
     * This is useful if the build is also using plugins such as the maven-gwt-plugin, that scans for resources, and
     * skips a step if none are found. Otherwise, setting this to false could well cause build failures.
     */
    @Parameter(property = "maven.clover.copyExcludedFiles", defaultValue = "true")
    protected boolean copyExcludedFiles = true;

    /**
     * <p>The configuration for distributed coverage collection by Clover.</p>
     * <p>If present, default values will be used and coverage will be collected across JVMs.</p>
     * <p>Optional nested elements (and their defaults) of distributedCoverage are:</p>
     * <ul>
     * <li>host - the host name of the JVM running the tests. default: <b>localhost</b></li>
     * <li>port - the port that Clover can bind to in the host JVM. default: <b>1198</b></li>
     * <li>numClients - the number of clients expected to attach to the Test JVM. The test JVM will wait until numClients
     * have connected before continuing. default: <b>0</b></li>
     * <li>timeout - the amount of time to wait for a response from a remote JVM before shunning it. default: <b>5000</b></li>
     * <li>retryPeriod - the amount of time a client should wait between reconnect attempts. default: <b>1000</b></li>
     * </ul>
     */
    @Parameter
    protected DistributedCoverage distributedCoverage;

    /**
     * The character encoding to use when parsing source files.
     */
    @Parameter(property = "maven.clover.encoding")
    protected String encoding;

    /**
     * The list of file to exclude from the instrumentation. Patterns are resolved against source roots.
     */
    @Parameter
    protected Set<String> excludes = new HashSet<>();

    /**
     * The comma seperated list of file to exclude from the instrumentation. Patterns are resolved against source roots.
     */
    @Parameter(property = "maven.clover.excludesList")
    protected String excludesList = null;

    /**
     * The file containing a list of file paths, separated by new line, to exclude from the instrumentation. Patterns are resolved against source roots.
     * See also {@link #excludes} and {@link #excludesList}
     */
    @Parameter(property = "maven.clover.excludesFile")
    protected String excludesFile = null;

    /**
     * The <a href="https://openclover.org/doc/manual/latest/ant--flush-policy.html">Clover flush policy</a> to use.
     * Valid values are <code>directed</code>, <code>interval</code> and <code>threaded</code>.
     */
    @Parameter(property = "maven.clover.flushPolicy", defaultValue = "threaded")
    protected String flushPolicy;

    /**
     * <p>By default, Clover Maven Plugin generates the <code>${java.io.tmpdir}/grover*.jar</code> file during setup,
     * which is next being added as the dependent artifact to the build. As the file has generated, unique
     * name and the jar is not being removed at the end of the build, these files can litter the temporary
     * directory.</p>
     * <p>By setting this parameter you can:</p>
     * <p> a) specify constant file name for generated artifact,</p>
     * <p> b) choose location different than ${java.io.tmpdir}.</p>
     * <p>However, you must ensure that:</p>
     * <p> a) grover.jar will not be deleted till end of the build (for example don't put into ./target directory
     * and next run <code>mvn clover:setup clean</code>)</p>
     * <p> b) grover.jar will not be shared among builds with different Clover Maven Plugin versions used (for
     * example if ProjectA uses Clover v 3.1.8 and ProjectB uses Clover v 3.1.9 then they shall have different
     * <code>groverJar</code> locations defined)</p>
     *
     * @since 3.1.8
     */
    @Parameter(property = "maven.clover.groverJar")
    protected File groverJar;

    /**
     * The list of file to include in the instrumentation. Patterns are resolved against source roots.
     * Defaults are '**&#47;*.java, **&#47;*.groovy' which are overwritten if &lt;includes&gt; is set by the user
     */
    @Parameter
    protected Set<String> includes = new HashSet<>(Arrays.asList("**/*.java", "**/*.groovy"));

    /**
     * The comma seperated list of files to include in the instrumentation. Patterns are resolved against source roots.
     * Defaults are **.java which are overwritten if &lt;includes&gt; is set by the user
     */
    @Parameter(property = "maven.clover.includesList")
    protected String includesList = null;

    /**
     * The file containing a list of file paths, separated by new line, to include in the instrumentation. Patterns are resolved against source roots.
     * See also {@link #includes} and {@link #includesList}
     */
    @Parameter(property = "maven.clover.includesFile")
    protected String includesFile = null;

    /**
     * <p><b>Till 3.1.11:</b> whether the Clover plugin should instrument all source roots (for example
     * <code>src/main/java, src/main/groovy, target/generated-sources</code>, so including the generated sources)
     * or whether it should only instrument the main source root (usually <code>src/main/java</code>).</p>
     * <p><b>Since 3.1.12:</b> whether the Clover plugin should instrument all source roots (for example
     * <code>src/main/java, src/main/groovy, target/generated-sources</code>, so including the generated sources)
     * or whether it should instrument non-generated source roots (i.e. all roots except <code>target/generated-sources/*</code>)</p>
     */
    @Parameter(property = "maven.clover.includesAllSourceRoots", defaultValue = "false")
    protected boolean includesAllSourceRoots;

    /**
     * Whether the Clover plugin should instrument test source roots.
     */
    @Parameter(property = "maven.clover.includesTestSourceRoots", defaultValue = "true")
    protected boolean includesTestSourceRoots;

    /**
     * <p>The level to instrument to. Valid values are 'method' or 'statement'. Default is 'statement'.</p>
     * <p>Setting this to 'method' greatly reduces the overhead of enabling Clover, however limited or no reporting is
     * available. The current use of setting this to method is for Test Optimization only.</p>
     */
    @Parameter(property = "maven.clover.instrumentation", defaultValue = "statement")
    protected String instrumentation;

    /**
     * <p>Define whether lambda functions shall be instrumented: Valid values are:</p>
     * <ul>
     * <li>none - do not instrument lambda functions (note: statements inside lambdas will become a part of a parent function)</li>
     * <li>expression - instrument only expression-like lambdas, e.g. <code>(a,b) -> a + b</code></li>
     * <li>block - instrument block lambdas, e.g. <code>() -> { foo(); }</code></li>
     * <li>all_but_reference - instrument lambdas written in any form except method references, e.g. <code>Math::abs</code></li>
     * <li>all - instrument all forms of lambda functions</li>
     * </ul>
     * <p>Default is 'all' for 3.2.2-4.0.2 and 'none' since 4.0.3.</p>
     * <p>IMPORTANT: Due to Clover's restrictions related with code instrumentation and javac compiler's type inference
     * capabilities, you may get compilation errors when expression-like lambda functions are passed to generic methods
     * or types. In such case disable instrumentation of expression-like form (i.e. use the 'none' or 'block' setting).
     * See the <a href="https://openclover.org/doc/manual/latest/kb--java-8-code-instrumented-by-clover-fails-to-compile.html">
     * Java 8 code instrumented by Clover fails to compile</a> Knowledge Base article for more details.
     * </p>
     *
     * @since 3.2.2
     */
    @Parameter(property = "maven.clover.instrumentLambda", defaultValue = "none")
    private String instrumentLambda;

    /**
     * <p>Which Java language level Clover shall use to parse sources. Valid values are: 8-17.</p>
     * <p>By default Clover instruments using the highest language level supported.</p>
     */
    @Parameter(property = "maven.clover.jdk")
    protected String jdk;

    /**
     * <p>Specifies the custom method contexts to use for filtering specific methods from Clover reports.</p>
     * e.g. <pre>&lt;main&gt;public static void main\(String args\[\]\).*&lt;/main&gt;</pre>
     * <p>will define the context called 'main' which will match all public static void main methods.</p>
     */
    @Parameter
    protected Map<String, String> methodContexts = new HashMap<>();

    /**
     * <p>Specifies the custom method contexts to use for filtering specific methods from Clover reports.
     * This is more detailed format compared to methodContexts, which allows to set also code metrics to be
     * matched. Example:</p>
     * <pre>
     * &lt;methodWithMetricsContexts&gt;
     *     &lt;methodWithMetricsContext&gt;
     *         &lt;name&gt;simpleGetter&lt;/name&gt; &lt;!-- (mandatory) --&gt;
     *         &lt;regexp&gt;public .* get.*\(\)&lt;/regexp&gt; &lt;!-- (mandatory) --&gt;
     *         &lt;maxComplexity&gt;1&lt;/maxComplexity&gt; &lt;!-- at most 1 cycle (optional) --&gt;
     *         &lt;maxStatements&gt;1&lt;/maxStatements&gt; &lt;!-- at most 1 statement (optional) --&gt;
     *         &lt;maxAggregatedComplexity&gt;2&lt;/maxAggregatedComplexity&gt; &lt;!-- no more than 2 cycles including inline classes (optional) --&gt;
     *         &lt;maxAggregatedStatements&gt;10&lt;/maxAggregatedStatements&gt; &lt;!-- no more than 10 statements including inline classes (optional) --&gt;
     *     &lt;/methodWithMetricsContext&gt;
     *     &lt;!-- can add more methodWithMetricsContext --&gt;
     * &lt;/methodWithMetricsContexts&gt;
     * </pre>
     * <p>will define a context called 'simpleGetter' which matches all public getXyz() methods containing at most one
     * statement; this statement may contain more complex logic (an anonymous inline class) but not bigger than 9
     * statements.</p>
     */
    @Parameter
    protected Set<MethodWithMetricsContext> methodWithMetricsContexts = new HashSet<>();

    /**
     * If set to 'false', test results will not be recorded; instead, results can be added via the
     * &lt;testResults&gt; fileset at report time. Useful when a test uses a custom
     * Rule expecting an exception, which OpenClover cannot recognize.
     */
    @Parameter(property = "maven.clover.recordTestResults", defaultValue = "true")
    protected boolean recordTestResults = true;

    /**
     * <p>Try to protect your build from installing instrumented artifacts into local ~/.m2 cache
     * or deploying them to a binaries repository. If this option is enabled, Clover will fail a build whenever
     * it detects that 'install' or 'deploy' phase is about to be called. It will also fail a build if
     * it detects that an artifact having multiple classifiers (e.g. "-clover-tests.jar"), which are not supported by
     * Maven, is about to be installed under original name (e.g. "-tests.jar").</p>
     * <p>Please note that this flag may not protect from all possible cases.</p>
     *
     * @since 4.0.4
     */
    @Parameter(property = "maven.clover.repositoryPollutionProtection", defaultValue = "false")
    protected boolean repositoryPollutionProtection;

    /**
     * When creating the clover.jar dependency, what scope to use.
     * This may be one of: compile, test, provided etc. If not specified - provided will be used.
     */
    @Parameter(property = "maven.clover.scope")
    protected String scope;

    /**
     * <p>If set to <code>true</code>, Clover will add several properties to the build configuration which
     * disable a build failure for following plugins:</p>
     * <ul>
     * <li>maven-surefire-plugin (maven.test.failure.ignore=true)</li>
     * <li>maven-failsafe-plugin (maven.test.failure.ignore=true)</li>
     * <li>maven-checkstyle-plugin (checkstyle.failOnViolation=false)</li>
     * <li>maven-pmd-plugin (pmd.failOnViolation=false)</li>
     * </ul>
     * <p>Thanks to this, build continues despite test failures or code validation failures and thus
     * it is possible to generate a Clover coverage report for failed tests at the end of the build.</p>
     * <p>Note: before version 3.1.9 the <i>testFailureIgnore</i> property was set to <i>true</i> for
     * the forked Clover lifecycle ('instrument' goal) for 'test' and 'integration-test' phases. Since
     * 3.1.9 it is no longer set.</p>
     *
     * @since 3.1.9
     */
    @Parameter(property = "maven.clover.setTestFailureIgnore", defaultValue = "false")
    protected boolean setTestFailureIgnore;

    /**
     * <p>By default, Clover Maven Plugin generates the <code>${java.io.tmpdir}/grover*.jar</code> file during setup,
     * which is next being added as the dependent artifact to the build. As the file has generated, unique
     * name and the jar is not being removed at the end of the build, these files can litter the temporary
     * directory.</p>
     * <p>In case when there is no Groovy code in the project, this parameter can be set to <code>true</code> in order
     * to disable generation of grover.jar artifact.</p>
     *
     * @since 3.1.8
     */
    @Parameter(property = "maven.clover.skipGroverJar", defaultValue = "false")
    protected boolean skipGroverJar = false;

    /**
     * Specifies the custom statement contexts to use for filtering specific statements from Clover reports.
     * e.g.<pre>&lt;log&gt;^LOG\..*&lt;/log&gt;</pre>
     * defines a statement context called "log" which matches all LOG statements.
     */
    @Parameter
    protected Map<String, String> statementContexts = new HashMap<>();

    /**
     * Sets the granularity in milliseconds of the last modification date for testing whether a source needs reinstrumentation.
     */
    @Parameter(property = "maven.clover.staleMillis", defaultValue = "0")
    protected int staleMillis;

    /**
     * Specifies a custom test detector configuration. Useful in case your tests are not following JUnit/TestNG
     * naming convention. Example:
     *
     * <pre>
     * &lt;testSources&gt;
     *    &lt;includes&gt;
     *        &lt;include&gt;**&#47;*&lt;/include&gt;
     *        &lt;include&gt;*WebTest.java&lt;/include&gt;
     *        &lt;include&gt;**&#47;*IT.java&lt;/include&gt;
     *    &lt;/includes&gt;
     *    &lt;excludes&gt;
     *        &lt;exclude&gt;deprecated/**&lt;/exclude&gt;
     *    &lt;/excludes&gt;
     *    &lt;testClasses&gt;
     *        &lt;testClass&gt; &lt;!-- 0..N occurrences --&gt;
     *            &lt;name&gt;.*Test&lt;/name&gt;
     *            &lt;super&gt;WebTest&lt;/super&gt;
     *            &lt;annotation&gt;@Repeat&lt;/annotation&gt;
     *            &lt;package&gt;org\.openclover\..*&lt;/package&gt;
     *            &lt;tag&gt;@chrome&lt;/tag&gt;
     *            &lt;testMethods&gt; &lt;!-- 0..N occurrences --&gt;
     *               &lt;testMethod&gt;
     *                   &lt;name&gt;check.*&lt;/name&gt;
     *                   &lt;annotation&gt;@Test&lt;/annotation&gt;
     *                   &lt;tag&gt;@web&lt;/tag&gt;
     *                   &lt;returnType&gt;void&lt;/returnType&gt;
     *               &lt;/testMethod&gt;
     *            &lt;/testMethods&gt;
     *        &lt;/testClass&gt;
     *    &lt;/testClasses&gt;
     * &lt;/testSources&gt;
     * </pre>
     *
     * Note: every tag is optional.
     *
     * @since 4.4.0
     */
    @Parameter
    protected TestSources testSources;

    /**
     * Whether to include the -clover classifier on artifacts.
     */
    @Parameter(property = "maven.clover.useCloverClassifier", defaultValue = "true")
    protected boolean useCloverClassifier;

    /**
     * Use the fully qualified package name for java.lang.* classes.
     */
    @Parameter(property = "maven.clover.useFullyQualifiedJavaLang", defaultValue = "true")
    protected boolean useFullyQualifiedJavaLang;

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Used to learn about lifecycles and phases
     */
    @Component
    private LifecycleExecutor lifecycleExecutor;

    /**
     * Used to learn about current build session.
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    /**
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void execute() throws MojoExecutionException {
        super.execute();
        if (repositoryPollutionProtection) {
            final BuildLifecycleAnalyzer lifecycleAnalyzer = new BuildLifecycleAnalyzer(
                    getLog(), lifecycleExecutor, mavenProject, mavenSession);
            failIfDeployPhaseIsPresent(lifecycleAnalyzer);
            failIfInstallPhaseIsPresent(lifecycleAnalyzer);
            failIfCustomClassifierIsPresent();
        }
    }

    protected abstract boolean shouldRedirectArtifacts();

    protected abstract boolean shouldRedirectOutputDirectories();

    @Override
    public boolean isCopyExcludedFiles() {
        return copyExcludedFiles;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public DistributedCoverage getDistributedCoverage() {
        return distributedCoverage;
    }

    @Override
    public Set<String> getExcludes() {
        if (excludesList == null && excludesFile == null) {
            return excludes;
        } else if (excludesFile != null) {
            try {
                return readPathPatternsFromFile(excludesFile);
            } catch (IOException e) {
                getLog().error("Could not read excludesFile: " + excludesFile, e);
                return Collections.emptySet();
            }
        } else {
            excludes.addAll(Arrays.asList(excludesList.split(",")));
            return excludes;
        }
    }

    @Override
    public String getFlushPolicy() {
        return this.flushPolicy;
    }

    @Override
    public Set<String> getIncludes() {
        if (includesList == null && includesFile == null) {
            return this.includes;
        } else if (includesFile != null) {
            try {
                return readPathPatternsFromFile(includesFile);
            } catch (IOException e) {
                getLog().error("Could not read includesFile: " + includesFile, e);
                return Collections.emptySet();
            }
        } else {
            return new HashSet<>(Arrays.asList(includesList.split(",")));
        }
    }

    @Override
    public String getInstrumentation() {
        return instrumentation;
    }

    @Override
    public String getInstrumentLambda() {
        return instrumentLambda;
    }

    @Override
    public String getJdk() {
        return this.jdk;
    }

    @Override
    public Map<String, String> getMethodContexts() {
        return methodContexts;
    }

    @Override
    public Set<MethodWithMetricsContext> getMethodWithMetricsContexts() {
        return methodWithMetricsContexts;
    }

    @Override
    public Map<String, String> getStatementContexts() {
        return statementContexts;
    }

    @Override
    public int getStaleMillis() {
        return staleMillis;
    }

    @Override
    public boolean isIncludesAllSourceRoots() {
        return this.includesAllSourceRoots;
    }

    @Override
    public boolean isUseFullyQualifiedJavaLang() {
        return useFullyQualifiedJavaLang;
    }

    @Override
    public TestSources getTestSources() {
        return testSources;
    }

    @Override
    public boolean isRecordTestResults() {
        return recordTestResults;
    }

    private static final String PROTECTION_ENABLED_MSG = "Clover's repository pollution protection is enabled. ";

    private static final String DISABLING_PROTECTION_MSG =
            "You can also disable repository pollution protection (-Dmaven.clover.repositoryPollutionProtection=false) if this is intentional.";

    /**
     * Read list of file paths to exclude/include from file
     *
     * @param file path to external file with list of files to exclude/include separated by new line
     * @return set of files to include/exclude
     * @throws IOException if it can't read external file
     */
    private Set<String> readPathPatternsFromFile(final String file) throws IOException {
        Set<String> files = new HashSet<>();
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
                files.add(line);
            }
        } finally {
            IOStreamUtils.close(br);
        }
        return files;
    }

    /**
     * Check if the build life cycle contains the 'install' phase.
     * 
     * @param lifecycleAnalyzer analyser
     * @throws org.apache.maven.plugin.MojoExecutionException if 'install' phase is present
     */
    protected void failIfInstallPhaseIsPresent(final BuildLifecycleAnalyzer lifecycleAnalyzer) throws MojoExecutionException {
        if (lifecycleAnalyzer.isInstallPresent() && (!useCloverClassifier || !shouldRedirectArtifacts())) {
            throw new MojoExecutionException(PROTECTION_ENABLED_MSG
                    + "Your build runs 'install' phase which can put instrumented JARs into ~/.m2 local cache. "
                    + "In order to fix this: \n"
                    + " - run a build till the 'verify' phase (the latest)\n"
                    + " - check if some build plug-in does not fork a parallel build cycle which runs till the 'install' phase\n"
                    + DISABLING_PROTECTION_MSG);
        }
    }

    /**
     * Check if the build life cycle contains the 'deploy' phase.
     *
     * @param lifecycleAnalyzer analyser
     * @throws org.apache.maven.plugin.MojoExecutionException if 'deploy' phase is present
     */
    protected void failIfDeployPhaseIsPresent(final BuildLifecycleAnalyzer lifecycleAnalyzer) throws MojoExecutionException {
        if (lifecycleAnalyzer.isDeployPresent() && (!useCloverClassifier || !shouldRedirectArtifacts())) {
            throw new MojoExecutionException(PROTECTION_ENABLED_MSG
                    + "Your build runs 'deploy' phase which can upload instrumented JARs into your repository. "
                    + "In order to fix this: \n"
                    + " - run a build till the 'verify' phase (the latest)\n"
                    + " - check if some build plug-in does not fork a parallel build cycle which runs till the 'deploy' phase\n"
                    + DISABLING_PROTECTION_MSG);
        }
    }

    /**
     * Check if an artifact has a custom classifier (except the 'javadoc' and 'sources' ones).
     * If a custom classifier is present then adding a second 'clover' classifier may not work correctly
     * as Maven does not support multiple classifiers.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException if custom classifier is present
     */
    protected void failIfCustomClassifierIsPresent() throws MojoExecutionException {
        final String classifier = getProject().getArtifact().getClassifier();
        final boolean customClassifierUsed = StringUtils.isNotEmpty(classifier)
                && !"javadoc".equals(classifier)
                && !"sources".equals(classifier);
        if (customClassifierUsed && useCloverClassifier && shouldRedirectArtifacts()) {
            throw new MojoExecutionException(PROTECTION_ENABLED_MSG
                    + "Your build produces an artifact (" + getProject().getArtifact() + ") with a custom classifier. "
                    + "As Maven does not support multiple "
                    + "classifiers for an artifact, appending second 'clover' classifier may not be handled correctly. "
                    + "You can: \n - remove a custom classifier or\n - configure Clover to not append the '-clover' classifier \n"
                    + "to fix it. You can also disable pollution protection "
                    + "(-Dmaven.clover.repositoryPollutionProtection=false) if you know "
                    + "that it doesn't affect your build. ");
        }
    }

}
