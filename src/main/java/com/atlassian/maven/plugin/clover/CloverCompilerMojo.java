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

    static Date START_DATE; 

    public void execute() throws MojoExecutionException {
        
        // store the start time of the build. ie - the very first compilation with clover.
        MavenProject firstProject = (MavenProject) getReactorProjects().get(0);
        if (firstProject == getProject()) {
            START_DATE = new Date();
        }

        super.execute();
    }

    protected void redirectOutputDirectories() {

    }

    protected void redirectArtifact() {

    }
}
