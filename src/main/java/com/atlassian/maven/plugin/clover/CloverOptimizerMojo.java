package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.cenqua.clover.CloverNames;
import com.cenqua.clover.types.CloverOptimizedTestSet;
import com.cenqua.clover.types.CloverAlwaysRunTestSet;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Sets the 'test' property on the project which is used by the maven-surefire-plugin to determine which tests are run.
 * If a snapshot file from a previous build, is found, that will be used to determine what tests should be run.
 *
 * @goal optimize
 * @phase process-test-classes
 */
public class CloverOptimizerMojo extends AbstractCloverMojo {

    /**
     * The number of builds to run, before the snapshot file gets deleted.
     * <p/>
     * The snapshot stores the mapping between your test cases and source code. Over time, this becomes stale,
     * so it is recommended to regenerate this file, by running all tests, on a regular basis.
     *
     * @parameter expression="${maven.clover.fullRunEvery}" default-value="10"
     */
    private int fullRunEvery;

    /**
     * A list of Tests to include for build optimization.
     * If not supplied, the includes specified in the maven-surefire-plugin's configuration will be used.
     *
     * @parameter
     */
    private List optimizeIncludes;


    /**
     * A list of Tests to exclude from build optimization.
     * If not supplied, the excludes specified in the maven-surefire-plugin's configuration will be used.
     *
     * @parameter
     */
    private List optimizeExcludes;

    /**
     * A list of Tests which should always be run. ie they will never be optimized away.
     * 
     * @parameter
     */
    private List alwaysRunTests;

    /**
     * <b>NOTE:</b> This currently has no effect, because the maven-surefire-plugin re-orders the tests alphabetically.
     *
     * This controls how Clover optimizes your tests.
     *
     * By default - clover excludes any test case it deems as irrelevant to any changes made to your source code.
     *
     * "failfast" - (default) ensures your build FAILs fast ie: tests relevant to code changes are run first
     *
     * "random" - tests will be shuffled before run. Can be used to determine inter-test-dependencies.
     *
     * @parameter expression="${maven.clover.ordering}"
     */
    private String ordering;

    /**
     * Toggles whether or not build optimization is to be done or not.
     *
     * @parameter expression="${maven.clover.optimize.enabled}" default-value="true"
     */
    private boolean enabled;


    /**
     * Controls whether or not to exclude tests that do not cover any modified files.
     *
     * If false, (and ordering is not random or original), Clover will not exclude any of the tests. Instead, they
     * will be run in an optimal order to ensure the build fails as fast as possible. ie - tests that cover modify code
     * first, then ascending by test time.
     * 
     * @parameter expression="${maven.clover.optimize.minimize}" default-value="true"
     *
     */
    private boolean minimize;

    /**
     * The default test patterns to include.
     */
    private static final List DEFAULT_INCLUDES = Arrays.asList(new String[]{"**/Test*.java", "**/*Test.java", "**/*TestCase.java"});


    public void execute() throws MojoExecutionException {

        if (skip) {
            getLog().info("Skipping build optimization.");
            return;
        }

        // if there are no source files, then skip this mojo
        final String sourceDirectory = getProject().getBuild().getSourceDirectory();
        final String testSourceDirectory = getProject().getBuild().getTestSourceDirectory();
        if (!new File(sourceDirectory).exists() && !new File(testSourceDirectory).exists()) {
            getLog().info(sourceDirectory + " and " + testSourceDirectory + " do not exist. No optimization will be done for: " + getProject().getArtifactId());
            return;
        }

        final Project antProj = new Project();
        antProj.init();
        antProj.addBuildListener(new MvnLogBuildListener(getLog()));


        final List optimizedTests = configureOptimisedTestSet(antProj);

        StringBuffer testPattern = new StringBuffer();
        for (Iterator iterator = optimizedTests.iterator(); iterator.hasNext();) {
            Resource test = (Resource) iterator.next();
            getLog().debug("Running TEST: " + test.getName());
            testPattern.append(test.getName());
            testPattern.append(",");
        }
        getLog().debug("Setting test property to: '" + testPattern + "'");

        if (optimizedTests.size() == 0) { // ensure surefire wont fail if we run no tests
            getProject().getProperties().put("failIfNoTests", "false");
        }

        getProject().getProperties().put("test", testPattern.toString());
    }

    private List configureOptimisedTestSet(Project antProj) {

        List surefireIncludes = null;
        List surefireExcludes = null;

        // lookup the surefire-plugin
        final Plugin surefirePlugin = lookupSurefirePlugin();

        if (surefirePlugin != null) {
            final Xpp3Dom config = (Xpp3Dom) surefirePlugin.getConfiguration();
            if (config != null) {
                // get the includes and excludes from the surefire plugin
                surefireIncludes = extractNestedStrings("includes", config);
                surefireExcludes = extractNestedStrings("excludes", config);
            }
        }

        final List includes = optimizeIncludes != null ? optimizeIncludes : (surefireIncludes != null) ? surefireIncludes : DEFAULT_INCLUDES;
        final List excludes = optimizeExcludes != null ? optimizeExcludes : surefireExcludes;

        final CloverOptimizedTestSet testsToRun = new CloverOptimizedTestSet();
        testsToRun.setProject(antProj);
        testsToRun.setLogger(new MvnLogger(getLog()));
        testsToRun.setFullRunEvery(fullRunEvery);
        testsToRun.setSnapshotFile(snapshot);
        if (ordering != null) {
            CloverOptimizedTestSet.TestOrdering order = new CloverOptimizedTestSet.TestOrdering();
            order.setValue(ordering);
            testsToRun.setOrdering(order);
        }
        testsToRun.setMinimize(minimize);
        testsToRun.setEnabled(enabled);

        antProj.setProperty(CloverNames.PROP_INITSTRING, resolveCloverDatabase());
        antProj.setName(getProject().getName());

        final List testSources = getProject().getTestCompileSourceRoots();

        for (Iterator iterator = testSources.iterator(); iterator.hasNext();) {
            String testRoot = (String) iterator.next();
            final File testRootDir = new File(testRoot);
            if (!testRootDir.exists()) {
                // if the test dir does not exist, do not add this as a fileset.
                continue;
            }

            FileSet testFileSet = new FileSet();
            testFileSet.setProject(antProj);

            testFileSet.setDir(testRootDir);


            testFileSet.appendIncludes((String[]) includes.toArray(new String[includes.size()]));
            getLog().debug("Appending includes: " + includes);

            if (excludes != null && excludes.size() > 0) {
                testFileSet.appendExcludes((String[]) excludes.toArray(new String[excludes.size()]));
                getLog().debug("Appending excludes: " + excludes);
            }

            testsToRun.add(testFileSet);

            if (alwaysRunTests != null) {
                // create  fileset
                final FileSet alwaysRunFileSet = new FileSet();
                alwaysRunFileSet.setProject(antProj);
                alwaysRunFileSet.setDir(testRootDir);
                alwaysRunFileSet.appendIncludes((String[]) alwaysRunTests.toArray(new String[alwaysRunTests.size()]));

                // add it to an AlwaysRunTestSet
                final CloverAlwaysRunTestSet alwaysRunTestSet = new CloverAlwaysRunTestSet();
                alwaysRunTestSet.setProject(antProj);
                alwaysRunTestSet.add(alwaysRunFileSet);

                // then add that to the OptimizedTestSet
                testsToRun.add(alwaysRunTestSet);
            }

        }
        return testsToRun.getOptimizedTestResource();
    }

    /**
     * Extracts nested values from the given config object into a List.
     *
     * @param childname the name of the first subelement that contains the list
     * @param config    the actual config object
     */
    private List extractNestedStrings(String childname, Xpp3Dom config) {

        final Xpp3Dom subelement = config.getChild(childname);
        if (subelement != null) {
            List result = new LinkedList();
            final Xpp3Dom[] children = subelement.getChildren();
            for (int i = 0; i < children.length; i++) {
                final Xpp3Dom child = children[i];
                result.add(child.getValue());
            }
            getLog().info("Extracted strings: " + result);
            return result;
        }

        return null;
    }

    private Plugin lookupSurefirePlugin() {

        final String key = "org.apache.maven.plugins:maven-surefire-plugin";

        final MavenProject mavenProject = getProject();
        if (mavenProject == null) {
            getLog().warn("Maven execution project is null. Surefire configuration will be ignored.");
            return null;

        }
        List plugins = mavenProject.getBuildPlugins();

        for (Iterator iterator = plugins.iterator(); iterator.hasNext();) {
            Plugin plugin = (Plugin) iterator.next();
            if (key.equalsIgnoreCase(plugin.getKey())) {
                return plugin;
            }
        }
        return null;
    }
}
