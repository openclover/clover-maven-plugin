package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.cenqua.clover.tasks.CloverTestCheckpointTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;

import java.io.File;

/**
 * @goal checkpoint
 * @phase test
 */
public class CloverCheckPointMojo extends AbstractCloverMojo {

    /**
     *
     * @parameter expression="${maven.clover.span}" 
     */
    private String span;

    /**
     * The location to store the clover checkpoint file. This file needs to persist between builds to enable Clover's
     * build optimization feature.
     * 
     * @parameter expression="${maven.clover.checkpoint}"
     */
    private File checkpoint;

    public void execute() throws MojoExecutionException {

        if (skip) {
            getLog().info("Skipping checkpoint.");
        }

         // if there is no database, do not save a checkpoint
        if (!new File(getCloverDatabase()).exists()) {
            getLog().info(getCloverDatabase() + " does not exist. Skipping checkpoint creation.");
            return;
        }

        CloverTestCheckpointTask task = new CloverTestCheckpointTask();
        final Project antProj = new Project();
        antProj.init();
        task.setProject(antProj);
        getLog().info("Clover database at: " + getCloverDatabase());
        task.setInitString(getCloverDatabase());
        if (span != null) {
            task.setSpan(span);
        }

        task.setFile(checkpoint);
        getLog().info("Saving checkpoint.");
        antProj.addBuildListener(new MvnLogBuildListener(getLog()));
        task.init();        
        task.execute();
    }
}
