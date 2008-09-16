package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.cenqua.clover.tasks.CloverTestCheckpointTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;

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

    public void execute() throws MojoExecutionException {

        

        CloverTestCheckpointTask task = new CloverTestCheckpointTask();
        final Project antProj = new Project();
        antProj.init();
        task.setProject(antProj);
        task.init();
        getLog().info("Clover database at: " + getCloverDatabase());
        task.setInitString(getCloverDatabase());
        if (span != null) {
            task.setSpan(span);
        }
        getLog().info("Saving checkpoint.");
        antProj.addBuildListener(new MvnLogBuildListener(getLog()));
        task.execute();
    }
}
