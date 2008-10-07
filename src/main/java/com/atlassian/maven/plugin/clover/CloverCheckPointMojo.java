package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.cenqua.clover.tasks.CloverCheckpointTask;
import com.cenqua.clover.util.CheckpointDumper;
import com.cenqua.clover.CloverTestCheckpoint;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;

import java.io.File;
import java.util.Date;

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
     * If set to true, the state of the checkpoint file will be dumped to the console.
     *
     * @parameter expression="${maven.clover.checkpoint.debug}" default-value="false"
     */
    private boolean debug;

    public void execute() throws MojoExecutionException {

        // only run the checkpoint once, on the very last project.
        if (isSingleCloverDatabase() && getReactorProjects().get(getReactorProjects().size() - 1) != getProject()) {
            return;
        }

        if (skip) {
            getLog().info("Skipping checkpoint.");
            return;
        }

         // if there is no database, do not save a checkpoint
        if (!new File(resolveCloverDatabase()).exists()) {
            getLog().info(resolveCloverDatabase() + " does not exist. Skipping checkpoint creation.");
            return;
        }

        CloverCheckpointTask task = new CloverCheckpointTask();
        final Project antProj = new Project();
        antProj.init();
        antProj.addBuildListener(new MvnLogBuildListener(getLog()));
        task.setProject(antProj);
        task.init();        
        task.setInitString(resolveCloverDatabase());
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

        if (getLog().isDebugEnabled() || debug) {
            final String cpLocation = checkpoint != null ? checkpoint.getPath() : task.getInitString() + ".teststate";
            CheckpointDumper.printPretty(CloverTestCheckpoint.loadFrom(cpLocation), new MvnLogger(getLog()));
        }

    }
}
