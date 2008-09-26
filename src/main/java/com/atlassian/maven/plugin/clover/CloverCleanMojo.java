package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.taskdefs.Delete;

import java.io.File;

/**
 * The clover2:clean goal should be run directly from the command line,
 * when you are developing using the clover test runner optimizer.
 *
 * This mojo ensures that the file required by Clover to optimize your test is not deleted between builds.
 * Unfortunately, the implementation is not optimal. It will be a lot simpler however if
 * the patch attached to http://jira.codehaus.org/browse/MCLEAN-38 is applied. Then, this plugin can simply set the
 * maven.clean.excludes property.
 * 
 * @goal clean
 * @phase initialize
 */
public class CloverCleanMojo extends AbstractCloverMojo {

    /**
     * The location of the Checkpoint file. By default, this is next to the cloverDatabase.
     *
     * @parameter expression="${maven.clover.checkpointPattern}" default-value="**\/*.teststate"
     */
    private String checkpointPattern;

    /**
     * This is where build results go.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File directory;

    /**
     * This is where compiled classes go.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * This is where compiled test classes go.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    private File testOutputDirectory;

    /**
     * This is where the site plugin generates its pages.
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @required
     * @readonly
     * @since 2.1.1
     */
    private File reportDirectory;

    /**
     * The comma-delimited includes patterns to use when deleting the default directory locations.
     *
     * @parameter expression="${maven.clean.includes}"
     * @since 2.3
     */

    public void execute() throws MojoExecutionException {

        final Project project = new Project();
        project.setBasedir(getProject().getBasedir().getPath());
        project.addBuildListener(new MvnLogBuildListener(getLog()));
        project.init();
        // delete just the checkpoint
        removeWithFilter(directory, project);
        removeWithFilter(outputDirectory, project);
        removeWithFilter(testOutputDirectory, project);
        removeWithFilter(reportDirectory, project);
    }

    private void removeWithFilter(File path, Project project) throws MojoExecutionException {

        if (!path.exists() && !path.isDirectory()) {
            return;
        }
        Delete delete = new Delete();
        delete.setProject(project);
        delete.setIncludeEmptyDirs(true);
        delete.init();
        FileSet fileSet = new FileSet();
        fileSet.setProject(project);
        fileSet.setDir(path);
        fileSet.setExcludes(checkpointPattern);
        delete.addFileset(fileSet);
        delete.execute();
    }
}
