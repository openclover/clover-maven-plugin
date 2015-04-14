package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.atlassian.maven.plugin.clover.internal.ConfigUtil;
import com.atlassian.clover.CloverNames;
import com.atlassian.clover.ant.types.CloverOptimizedTestSet;
import com.atlassian.clover.ant.types.CloverAlwaysRunTestSet;
import com.atlassian.clover.util.FileUtils;
import com.google.common.collect.Iterables;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Sets the 'test' property on the project which is used by the maven-surefire-plugin to determine which tests are run.
 * If a snapshot file from a previous build, is found, that will be used to determine what tests should be run.
 *
 * @goal optimize
 * @phase process-test-classes
 */
public class CloverOptimizerMojo extends AbstractCloverMojo {

    /**
     * <p>The number of builds to run, before the snapshot file gets deleted.</p>
     * <p>The snapshot stores the mapping between your test cases and source code. Over time, this becomes stale,
     * so it is recommended to regenerate this file, by running all tests, on a regular basis.</p>
     *
     * @parameter expression="${maven.clover.fullRunEvery}" default-value="10"
     */
    private int fullRunEvery;

    /**
     * A list of Tests to include for build optimization.
     * If neither <i>optimizeIncludes</i> nor <i>optimizeExcludes</i> are supplied, then the
     * <i>includes</i> specified in the maven-surefire-plugin's configuration will be used.
     *
     * @parameter
     */
    private List<String> optimizeIncludes;


    /**
     * A list of Tests to exclude from build optimization.
     * If neither <i>optimizeIncludes</i> nor <i>optimizeExcludes</i> are supplied, then the
     * <i>excludes</i> specified in the maven-surefire-plugin's configuration will be used.
     *
     * @parameter
     */
    private List<String> optimizeExcludes;

    /**
     * A list of Tests which should always be run. ie they will never be optimized away.
     * 
     * @parameter
     */
    private List<String> alwaysRunTests;

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
    private static final List<String> DEFAULT_INCLUDES = Arrays.asList(new String[]{"**/Test*.java", "**/*Test.java", "**/*TestCase.java"});

    private static final String REGEX_START = "%regex[";
    private static final String REGEX_END = "]";

    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping build optimization.");
            return;
        }

        // if there are no source files, then skip this mojo
        final String sourceDirectory = getProject().getBuild().getSourceDirectory();
        final String testSourceDirectory = getProject().getBuild().getTestSourceDirectory();
        if (!new File(sourceDirectory).exists() && !new File(testSourceDirectory).exists()) {
            getLog().info(sourceDirectory + " and " + testSourceDirectory + " do not exist. No optimization will be done for: "
                    + getProject().getGroupId() + ":" + getProject().getArtifactId());
            return;
        }

        final Project antProj = new Project();
        antProj.init();
        antProj.addBuildListener(new MvnLogBuildListener(getLog()));

        final List<Resource> optimizedTests = configureOptimisedTestSet(antProj);
        final StringBuffer testPattern = new StringBuffer();
        for (final Resource test : optimizedTests) {
            getLog().debug("Running TEST: " + test.getName());
            testPattern.append(test.getName());
            testPattern.append(",");
        }
        getLog().debug("Setting test property to: '" + testPattern + "'");

        //Always set this to true because we can't be sure if the filtered list we have will result in no tests being run
        //because we matched classes under src/test/ which aren't unit tests
        getProject().getProperties().put("failIfNoTests", "false");
        if (optimizedTests.size() == 0) {
            // empty -Dtest values cause all tests to be run so let's put a dummy value
            getProject().getProperties().put("test", "clover/optimized/test/PlaceHolder.java");
            // ensure surefire wont fail if we run no tests
        } else {
            getProject().getProperties().put("test", testPattern.toString());
        }
    }

    protected List<Resource> configureOptimisedTestSet(final Project antProj) {
        List<String> includes = optimizeIncludes;
        List<String> excludes = optimizeExcludes;
        
        if (includes == null && excludes == null) {
            getLog().debug("No clover excludes or includes specified. Falling back to Surefire configuration.");

            final Plugin surefirePlugin = lookupSurefirePlugin();
            if (surefirePlugin != null) {
                includes = extractNestedStrings("includes", surefirePlugin);
                excludes = extractNestedStrings("excludes", surefirePlugin);
            }
            // If there are still no includes use the default ones
            if (includes == null) {
                includes = DEFAULT_INCLUDES;
            }
        }

        getLog().debug("Effective filtering: includes=" + includes + ", excludes=" + excludes);
        
        final CloverOptimizedTestSet testsToRun = new CloverOptimizedTestSet();
        testsToRun.setProject(antProj);
        testsToRun.setLogger(new MvnLogger(getLog()));
        testsToRun.setFullRunEvery(fullRunEvery);
        testsToRun.setDebug(debug);

        testsToRun.setSnapshotFile(new ConfigUtil(this).resolveSnapshotFile(snapshot));
        if (ordering != null) {
            final CloverOptimizedTestSet.TestOrdering order = new CloverOptimizedTestSet.TestOrdering();
            order.setValue(ordering);
            testsToRun.setOrdering(order);
        }
        testsToRun.setMinimize(minimize);
        testsToRun.setEnabled(enabled);

        antProj.setProperty(CloverNames.PROP_INITSTRING, resolveCloverDatabase());
        antProj.setName(getProject().getName());

        final List<String> testSources = getProject().getTestCompileSourceRoots();
        for (String testSource : testSources) {
            addTestRoot(antProj, includes, excludes, testsToRun, testSource);
        }
        return testsToRun.getOptimizedTestResource();
    }

    protected List<String> extractNestedStrings(final String elementName, final Plugin surefirePlugin) {
        final Xpp3Dom config = (Xpp3Dom) surefirePlugin.getConfiguration();
        return config == null ? null : extractNestedStrings(elementName, config);
    }

    /**
     * Extracts nested values from the given config object into a List.
     *
     * @param childname the name of the first subelement that contains the list
     * @param config    the actual config object
     */
    static List<String> extractNestedStrings(final String childname, final Xpp3Dom config) {
        final Xpp3Dom subelement = config.getChild(childname);
        if (subelement != null) {
            final List<String> result = new LinkedList<String>();
            final Xpp3Dom[] children = subelement.getChildren();
            for (final Xpp3Dom child : children) {
                result.add(child.getValue());
            }
            return result;
        }

        return null;
    }

    private void addTestRoot(final Project antProj, final List<String> includes, final List<String> excludes,
                             final CloverOptimizedTestSet testsToRun, final String testRoot) {
        final File testRootDir = new File(testRoot);
        if (!testRootDir.exists()) {
            // if the test dir does not exist, do not add this as a fileset.
            return;
        }

        getLog().info("Adding fileset: directory=" + testRootDir + ", includes=" + includes + ", excludes=" + excludes);

        testsToRun.add(createFileSet(antProj, testRootDir, includes, excludes));

        if (alwaysRunTests != null) {
            // create  fileset
            final FileSet alwaysRunFileSet = createFileSet(antProj, testRootDir, alwaysRunTests, null);

            // add it to an AlwaysRunTestSet
            final CloverAlwaysRunTestSet alwaysRunTestSet = new CloverAlwaysRunTestSet();
            alwaysRunTestSet.setProject(antProj);
            alwaysRunTestSet.add(alwaysRunFileSet);

            // then add that to the OptimizedTestSet
            testsToRun.add(alwaysRunTestSet);
        }
    }

    /**
     * Creates a FileSet for <code>antProject</code> and base <code>directory</code> having a list of files
     * to be included and excluded, according to <code>includes / excludes</code> wildcard patterns.
     *
     * @param antProject
     * @param directory
     * @param includes
     * @param excludes
     * @return FileSet
     */
    FileSet createFileSet(final Project antProject, final File directory, final List<String> includes, final List<String> excludes) {
        final FileSet testFileSet = new FileSet();
        testFileSet.setProject(antProject);
        testFileSet.setDir(directory);

        final List<String> includesExpanded = explodePaths(directory, includes);
        testFileSet.appendIncludes(Iterables.toArray(includesExpanded, String.class));

        if (excludes != null && !excludes.isEmpty()) {
            final List<String> excludesExpanded = explodePaths(directory, excludes);
            testFileSet.appendExcludes(Iterables.toArray(excludesExpanded, String.class));
        }
        return testFileSet;
    }

    /**
     * Search for maven-surefire-plugin in the list of build plugins. Returns a plugin instance or
     * <code>null</code> if not found.
     * @return Plugin maven-surefire-plugin or <code>null</code>
     */
    private Plugin lookupSurefirePlugin() {
        final String key = "org.apache.maven.plugins:maven-surefire-plugin";
        final MavenProject mavenProject = getProject();
        if (mavenProject == null) {
            getLog().warn("Maven execution project is null. Surefire configuration will be ignored.");
            return null;
        }

        final List<Plugin> plugins = mavenProject.getBuildPlugins();
        for (final Plugin plugin : plugins) {
            if (key.equalsIgnoreCase(plugin.getKey())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Resolves exact list of paths using the input <code>paths</code> list, because:
     *  1) we can have multiple 'includes' tags in Ant FileSet and
     *  2a) we can have multiple comma- or space-separated patterns in one 'includes'
     *  2b) we can have regular expression entered (surefire specific feature)
     *
     * For 2a) String.split() is used, for 2b) a directory scan is performed
     *
     * See:
     * <li>http://ant.apache.org/manual/Types/fileset.html</li>
     * <li>http://maven.apache.org/plugins/maven-surefire-plugin/examples/inclusion-exclusion.html</li>
     *
     * @param paths list of paths (single or separated by space or comma)
     * @return List&lt;String&gt;
     */
    static List<String> explodePaths(final File directory, final List<String> paths) {
        final List<String> explodedPaths = new LinkedList<String>();
        for (final String path : paths) {
            if (path.trim().startsWith("%regex[")) {
                splitPathByRegexp(directory, explodedPaths, path);
            } else {
                splitPathBySeparators(explodedPaths, path);
            }
        }

        return explodedPaths;
    }

    private static List<File> dirTreeMatchingPattern(final File dir, final Pattern pattern) {
        final List<File> matchedFiles = new LinkedList<File>();

        if (dir.isDirectory()) {
            // recursive search
            for (String fileName : dir.list()) {
                matchedFiles.addAll(dirTreeMatchingPattern(new File(dir, fileName), pattern));
            }
        } else {
            // add a file
            if (pattern.matcher(dir.getPath()).matches()) {
                matchedFiles.add(dir);
            }
        }

        return matchedFiles;
    }

    /**
     * Takes <code>pathRegex</code> regular expression in a form like "%regex[.*[Cat|Dog].*Test.*]" (as supported by
     * surefire plugin) and searches for all files in <code>directory</code> whose path name matches this expression.
     * Adds relative paths of found files to <code>outputList</code>.
     *
     * @param directory  directory to be scanned
     * @param outputList output list to which names of found files will be added
     * @param pathRegex  regular expression for file name
     */
    private static void splitPathByRegexp(final File directory, final List<String> outputList, final String pathRegex) {
        // extract regular expression from a path entry (we assume that there can be only one regexp)
        final String regex = pathRegex.substring(
                pathRegex.indexOf(REGEX_START) + REGEX_START.length(),
                pathRegex.lastIndexOf(REGEX_END));

        // create pattern for this regexp and find all files in directory matching it
        final Pattern pattern = Pattern.compile(regex);
        final List<File> matchedFiles = dirTreeMatchingPattern(directory, pattern);

        // convert File->String and add to output list
        for (final File file : matchedFiles) {
            outputList.add(FileUtils.getRelativePath(directory, file));
        }
    }

    private static void splitPathBySeparators(final List<String> outputList, final String path) {
        final String ANT_PATTERN_SEPARATOR = "[, ]";
        final String splittedPaths[] = path.split(ANT_PATTERN_SEPARATOR);
        for (String splittedPath : splittedPaths) {
            if (splittedPath.length() > 0) {
                outputList.add(splittedPath);
            }
        }
    }
}
