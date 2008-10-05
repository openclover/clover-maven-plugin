package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.cenqua.clover.tasks.CloverTestCheckpointTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @goal checkpoint
 * @phase post-integration-test
 * @aggregator
 */
public class CloverCheckPointMojo extends AbstractCloverMojo {

    /**
     *
     * @parameter expression="${maven.clover.span}" 
     */
    private String span;

    /**
     * The projects in the reactor for aggregation report.
     *
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List reactorProjects;

    public void execute() throws MojoExecutionException {

        // only run the checkpoint once, on the very last project.
        if (globalCheckpoint && reactorProjects.get(reactorProjects.size() - 1) != getProject()) {
            return;
        }

        if (skip) {
            getLog().info("Skipping checkpoint.");
            return;
        }

         // if there is no database, do not save a checkpoint
        if (!new File(getCloverDatabase()).exists()) {
            getLog().info(getCloverDatabase() + " does not exist. Skipping checkpoint creation.");
            return;
        }

        CloverTestCheckpointTask task = new CloverTestCheckpointTask();
        final Project antProj = new Project();
        antProj.init();
        antProj.addBuildListener(new MvnLogBuildListener(getLog()));
        task.setProject(antProj);
        task.init();        
        task.setInitString(getCloverDatabase());
        if (span != null) {
            task.setSpan(span);
        } else if (CloverCompilerMojo.START_DATE != null) {
            final long timeSinceStart = new Date().getTime() - CloverCompilerMojo.START_DATE.getTime();
            final String interval = ((timeSinceStart + 1000)/ 1000) + "s";
            getLog().info("No span specified, using span of: " + interval);
            task.setSpan(interval);
        }

        if (checkpoint != null) {
            getLog().info("Saving checkpoint to: " + checkpoint);
            task.setFile(checkpoint);
        }

        task.execute();
    }
}
