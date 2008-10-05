package com.atlassian.maven.plugin.clover;

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;
import java.util.Date;

/**
 * This mojo is to be used mainly for incremental instrumentation and compilation of Java source code.
 * <p/>
 * NB: This does not, nor should it, run in a forked lifecycle.
 *
 * @goal setup
 * @phase process-sources
 */
public class CloverCompilerMojo extends CloverInstrumentInternalMojo {

    /**
     * The projects in the reactor for aggregation report.
     * <p/>
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List reactorProjects;

    static Date START_DATE; 

    public void execute() throws MojoExecutionException {
        
        // store the start time of the build. ie - the very first compilation with clover.
        MavenProject firstProject = (MavenProject) reactorProjects.get(0);
        if (firstProject == getProject()) {
            START_DATE = new Date();
        }

        super.execute();
    }

    public String getCloverDatabase() {
        return globalCheckpoint ? getProject().getExecutionProject().getBuild().getDirectory() + "/clover/clover.db" 
                : super.getCloverDatabase();
    }

    protected void redirectOutputDirectories() {

    }

    protected void redirectArtifact() {

    }
}
