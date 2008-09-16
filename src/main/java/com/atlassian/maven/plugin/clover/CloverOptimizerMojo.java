package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.cenqua.clover.types.junit.CloverOptimisedTestSet;
import com.cenqua.clover.Logger;
import com.cenqua.clover.CloverNames;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Arrays;
import java.io.File;

/**
 * @goal optimize
 */
public class CloverOptimizerMojo extends AbstractCloverMojo {

    /**
     *
     * @parameter expression="${maven.clover.fullRunAfter}" default-value="10"
     *
     */
    private int fullRunAfter;

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

        final Project antProj = new Project();
        antProj.init();
        antProj.addBuildListener(new MvnLogBuildListener(getLog()));


        //testsToRun.setFullRunAfter(fullRunAfter);

        final List optimizedTests = configureOptimisedTestSet(antProj);

        StringBuffer testPattern = new StringBuffer();
        for (Iterator iterator = optimizedTests.iterator(); iterator.hasNext();) {
            Resource test = (Resource) iterator.next();
            getLog().info("Running TEST::: " + test.getName());
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
            // get the includes and excludes from the surefire plugin
            surefireIncludes = extractNestedStrings("includes", config);
            surefireExcludes = extractNestedStrings("excludes", config);
        }

        final List includes = optimizeIncludes != null ? optimizeIncludes : (surefireIncludes != null) ? surefireIncludes : DEFAULT_INCLUDES;
        final List excludes = optimizeExcludes != null ? optimizeExcludes : surefireExcludes;


        final CloverOptimisedTestSet testsToRun = new CloverOptimisedTestSet();
        testsToRun.setProject(antProj);
        testsToRun.setLoggerFactory(new MvnLogger.MvnLoggerFactory(getLog()));
        antProj.setProperty(CloverNames.PROP_INITSTRING, getCloverDatabase());
        antProj.setName(getProject().getName());
        
        
        final List testSources = getProject().getTestCompileSourceRoots();
        for (Iterator iterator = testSources.iterator(); iterator.hasNext();) {
            String testRoot = (String) iterator.next();
            FileSet testFileSet = new FileSet();
            testFileSet.setProject(antProj);
            testFileSet.setDir(new File(testRoot));


            testFileSet.appendIncludes((String[]) includes.toArray(new String[includes.size()]));
            getLog().info("INCLUDING: " + includes);

            if (excludes != null && excludes.size() > 0) {
                testFileSet.appendExcludes((String[]) excludes.toArray(new String[excludes.size()]));
                getLog().info("EXCLUDING: " + excludes);
            }

            testsToRun.add(testFileSet);
        }
        return testsToRun.getOptimisedTestResource();
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
            return result;
        }
        return null;
    }

    private Plugin lookupSurefirePlugin() {

        final MavenProject mavenProject = getProject().getExecutionProject();
        if (mavenProject == null) {
            getLog().warn("Maven execution project is null. Surefire configuration will be ignored.");
            return null;

        }
        List plugins = mavenProject.getBuildPlugins();


        for (Iterator iterator = plugins.iterator(); iterator.hasNext();) {
            Plugin plugin = (Plugin) iterator.next();
            if("org.apache.maven.plugins:maven-surefire-plugin".equalsIgnoreCase(plugin.getKey())) {
                return plugin;
            }
        }
        return null;
    }
}
