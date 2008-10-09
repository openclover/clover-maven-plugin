package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.cenqua.clover.CloverNames;
import com.cenqua.clover.types.CloverOptimizedTestSet;
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
 * @goal optimize
 * @phase process-test-classes
 */
public class CloverOptimizerMojo extends AbstractCloverMojo {

    /**
     *
     * @parameter expression="${maven.clover.fullRunEvery}" default-value="10"
     *
     */
    private int fullRunEvery;

    /**
     * @parameter 
     */
    private List optimizeIncludes;


    /**
     * @parameter
     */
    private List optimizeExcludes;

    /**
     * The default test patterns to include.
     */
    private static final List DEFAULT_INCLUDES = Arrays.asList(new String[] { "**/Test*.java", "**/*Test.java","**/*TestCase.java" });


    public void execute() throws MojoExecutionException {

        if (skip) {
            getLog().info("Skipping build optimization.");
        }

        // if there are no source files, then skip this mojo
        final String sourceDirectory = getProject().getBuild().getSourceDirectory();
        if (!new File(sourceDirectory).exists()) {
            getLog().info(sourceDirectory + " does not exist. No optimization will be done.");
            return;
        }

        final Project antProj = new Project();
        antProj.init();
        antProj.addBuildListener(new MvnLogBuildListener(getLog()));



        final List optimizedTests = configureOptimisedTestSet(antProj);

        StringBuffer testPattern = new StringBuffer();
        for (Iterator iterator = optimizedTests.iterator(); iterator.hasNext();) {
            Resource test = (Resource) iterator.next();
            getLog().info("Running TEST: " + test.getName());
            testPattern.append(test.getName());
            testPattern.append(",");
        }
        getLog().info("Setting test property to: " + testPattern);

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
            getLog().debug("INCLUDING: " + includes);

            if (excludes != null && excludes.size() > 0) {
                testFileSet.appendExcludes((String[]) excludes.toArray(new String[excludes.size()]));
                getLog().debug("EXCLUDING: " + excludes);
            }

            testsToRun.add(testFileSet);
        }
        return testsToRun.getOptimizedTestResource();
    }

    /**
     * Extracts nested values from the given config object into a List.
     * 
     * @param childname the name of the first subelement that contains the list
     * @param config the actual config object
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
            if(key.equalsIgnoreCase(plugin.getKey())) {
                return plugin;
            }
        }
        return null;
    }
}
