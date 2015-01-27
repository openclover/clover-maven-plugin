package com.atlassian.maven.plugin.clover.internal;

import com.atlassian.maven.plugin.clover.DistributedCoverage;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Common settings for clover2:instr / clover2:setup MOJOs.
 */
public class AbstractCloverInstrumentMojo extends AbstractCloverMojo implements CompilerConfiguration {
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
    protected long cloveredArtifactExpiryInMillis;

    /**
     * If set, then the maven-clover2-plugin will not copy files that were excluded, across to the target/clover directory.
     * This is useful if the build is also using plugins such as the maven-gwt-plugin, that scans for resources, and
     * skips a step if none are found. Otherwise, setting this to false could well cause build failures.
     *
     * @parameter expression="${maven.clover.copyExcludedFiles}" default-value="true"
     */
    protected boolean copyExcludedFiles = true;

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
    protected DistributedCoverage distributedCoverage;

    /**
     * The character encoding to use when parsing source files.
     *
     * @parameter expression="${maven.clover.encoding}"
     */
    protected String encoding;

    /**
     * The list of file to exclude from the instrumentation. Patterns are resolved against source roots.
     * @parameter
     */
    protected Set<String> excludes = new HashSet<String>();

    /**
     * The comma seperated list of file to exclude from the instrumentation. Patterns are resolved against source roots.
     * @parameter expression="${maven.clover.excludesList}"
     */
    protected String excludesList = null;

    /**
     * The <a href="http://confluence.atlassian.com/x/O4BOB">Clover flush policy</a> to use.
     * Valid values are <code>directed</code>, <code>interval</code> and <code>threaded</code>.
     *
     * @parameter expression="${maven.clover.flushPolicy}" default-value="threaded"
     */
    protected String flushPolicy;

    /**
     * <p>By default, Maven Clover Plugin generates the <code>${java.io.tmpdir}/grover*.jar</code> file during setup,
     * which is next being added as the dependent artifact to the build. As the file has generated, unique
     * name and the jar is not being removed at the end of the build, these files can litter the temporary
     * directory.</p>
     *
     * <p>By setting this parameter you can:</p>
     * <p> a) specify constant file name for generated artifact,</p>
     * <p> b) choose location different than ${java.io.tmpdir}.</p>
     *
     * <p>However, you must ensure that:</p>
     * <p> a) grover.jar will not be deleted till end of the build (for example don't put into ./target directory
     * and next run <code>mvn clover2:setup clean</code>)</p>
     * <p> b) grover.jar will not be shared among builds with different Maven Clover Plugin versions used (for
     * example if ProjectA uses Clover v 3.1.8 and ProjectB uses Clover v 3.1.9 then they shall have different
     * <code>groverJar</code> locations defined)</p>
     *
     * @parameter expression="${maven.clover.groverJar}"
     * @since 3.1.8
     */
    protected File groverJar;

    /**
     * The list of file to include in the instrumentation. Patterns are resolved against source roots.
     * Defaults are '**&#47;*.java, **&#47;*.groovy' which are overwritten if &lt;includes&gt; is set by the user
     *
     * @parameter
     */
    protected Set<String> includes = new HashSet<String>(Arrays.asList(new String[]{"**/*.java", "**/*.groovy" }));

    /**
     * The comma seperated list of files to include in the instrumentation. Patterns are resolved against source roots.
     * Defaults are **.java which are overwritten if &lt;includes&gt; is set by the user
     *
     * @parameter expression="${maven.clover.includesList}"
     */
    protected String includesList = null;

    /**
     * <p><b>Till 3.1.11:</b> whether the Clover plugin should instrument all source roots (for example
     * <code>src/main/java, src/main/groovy, target/generated-sources</code>, so including the generated sources)
     * or whether it should only instrument the main source root (usually <code>src/main/java</code>).</p>
     *
     * <p><b>Since 3.1.12:</b> whether the Clover plugin should instrument all source roots (for example
     * <code>src/main/java, src/main/groovy, target/generated-sources</code>, so including the generated sources)
     * or whether it should instrument non-generated source roots (i.e. all roots except <code>target/generated-sources/*</code>)</p>
     *
     * @parameter expression="${maven.clover.includesAllSourceRoots}" default-value="false"
     */
    protected boolean includesAllSourceRoots;

    /**
     * Whether the Clover plugin should instrument test source roots.
     * @parameter expression="${maven.clover.includesTestSourceRoots}" default-value="true"
     */
    protected boolean includesTestSourceRoots;

    /**
     * The level to instrument to. Valid values are 'method' or 'statement'. Default is 'statement'.
     *
     * Setting this to 'method' greatly reduces the overhead of enabling Clover, however limited or no reporting is
     * available. The current use of setting this to method is for Test Optimization only.
     *
     * @parameter expression="${maven.clover.instrumentation}"
     */
    protected String instrumentation;

    /**
     * <p>Define whether lambda functions shall be instrumented: Valid values are:</p>
     *
     * <ul>
     *     <li>none - do not instrument lambda functions (note: statements inside lambdas will become a part of a parent function)</li>
     *     <li>expression - instrument only expression-like lambdas, e.g. <code>(a,b) -> a + b</code></li>
     *     <li>block - instrument block lambdas, e.g. <code>() -> { foo(); }</code></li>
     *     <li>all - instrument all forms of lambda functions</li>
     * </ul>
     *
     * <p>Default is 'all' for 3.2.2-4.0.2 and 'none' since 4.0.3.</p>
     *
     * <p>IMPORTANT: Due to Clover's restrictions related with code instrumentation and javac compiler's type inference
     * capabilities, you may get compilation errors when expression-like lambda functions are passed to generic methods
     * or types. In such case disable instrumentation of expression-like form (i.e. use the 'none' or 'block' setting).
     * See the <a href="https://confluence.atlassian.com/display/CLOVERKB/Java+8+code+instrumented+by+Clover+fails+to+compile">
     * Java 8 code instrumented by Clover fails to compile</a> Knowledge Base article for more details.
     * </p>
     *
     * @parameter expression="${maven.clover.instrumentLambda}" default-value="none"
     * @since 3.2.2
     */
    private String instrumentLambda;

    /**
     * Which Java language level Clover shall use to parse sources. Valid values are:
     * <ul>
     *     <li>1.3</li>
     *     <li>1.4 (introduces 'assert' keyword)</li>
     *     <li>1.5 ('enum' keyword and generics)</li>
     *     <li>1.6 (no language changes)</li>
     *     <li>1.7 (String in switch, try with resources, binary literals, underscores in literals)</li>
     *     <li>1.8 (lambda expressions, default methods in interfaces)</li>
     * </ul>
     *
     * By default Clover instruments using the highest language level supported.
     *
     * @parameter expression="${maven.clover.jdk}"
     */
    protected String jdk;

    /**
     * Specifies the custom method contexts to use for filtering specific methods from Clover reports.
     *
     * e.g. <pre>&lt;main&gt;public static void main\(String args\[\]\).*&lt;/main&gt;</pre>
     * will define the context called 'main' which will match all public static void main methods.
     *
     * @parameter
     */
    protected Map<String,String> methodContexts = new HashMap<String, String>();

    /**
     * When creating the clover.jar dependency, what scope to use.
     * This may be one of: compile, test, provided etc. If not specified - provided will be used.
     *
     * @parameter expression="${maven.clover.scope}"
     */
    protected String scope;

    /**
     * <p>If set to <code>true</code>, Clover will add several properties to the build configuration which
     * disable a build failure for following plugins:</p>
     *
     * <ul>
     *  <li>maven-surefire-plugin (maven.test.failure.ignore=true)</li>
     *  <li>maven-failsafe-plugin (maven.test.failure.ignore=true)</li>
     *  <li>maven-checkstyle-plugin (checkstyle.failOnViolation=false)</li>
     *  <li>maven-pmd-plugin (pmd.failOnViolation=false)</li>
     * </ul>
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
    protected boolean setTestFailureIgnore;

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
    protected boolean skipGroverJar = false;

    /**
     * Specifies the custom statement contexts to use for filtering specific statements from Clover reports.
     *
     * e.g.<pre>&lt;log&gt;^LOG\..*&lt;/log&gt;</pre>
     * defines a statement context called "log" which matches all LOG statements.
     *
     * @parameter
     */
    protected Map<String,String> statementContexts = new HashMap<String, String>();

    /**
     * Sets the granularity in milliseconds of the last modification date for testing whether a source needs reinstrumentation.
     *
     * @parameter expression="${maven.clover.staleMillis}" default-value="0"
     */
    protected int staleMillis;

    /**
     * Whether or not to include the -clover classifier on artifacts.
     *
     * @parameter expression="${maven.clover.useCloverClassifier}" default-value="true"
     */
    protected boolean useCloverClassifier;
    /**
     * Use the fully qualified package name for java.lang.* classes.
     *
     * @parameter expression="${maven.clover.useFullyQualifiedJavaLang}" default-value="true"
     */
    protected boolean useFullyQualifiedJavaLang;


    public boolean isCopyExcludedFiles() {
        return copyExcludedFiles;
    }

    public String getEncoding() {
        return encoding;
    }

    public DistributedCoverage getDistributedCoverage() {
        return distributedCoverage;
    }

    public Set<String> getExcludes() {
        if (excludesList == null) {
            return excludes;
        } else {
            excludes.addAll(Arrays.asList(excludesList.split(",")));
            return excludes;
        }
    }

    public String getFlushPolicy() {
        return this.flushPolicy;
    }

    public Set<String> getIncludes() {
        if (includesList == null) {
            return this.includes;
        } else {
            return new HashSet<String>(Arrays.asList(includesList.split(",")));
        }
    }

    public String getInstrumentation() {
        return instrumentation;
    }

    public String getInstrumentLambda() {
        return instrumentLambda;
    }

    public String getJdk() {
        return this.jdk;
    }

    public Map<String, String> getMethodContexts() {
        return methodContexts;
    }

    public Map<String, String> getStatementContexts() {
        return statementContexts;
    }

    public int getStaleMillis() {
        return staleMillis;
    }

    public boolean isIncludesAllSourceRoots() {
        return this.includesAllSourceRoots;
    }

    public boolean isUseFullyQualifiedJavaLang() {
        return useFullyQualifiedJavaLang;
    }
}
