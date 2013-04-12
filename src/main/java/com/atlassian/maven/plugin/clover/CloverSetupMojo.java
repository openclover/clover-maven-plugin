package com.atlassian.maven.plugin.clover;

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.Date;

/**
 * <p>This mojo is to be used mainly for incremental instrumentation and compilation of Java source code.</p>
 *
 * <p>NB: This does not, nor should it, run in a forked lifecycle.</p>
 *
 * <p>This mojo should certainly *not* be called during a release build. This mojo instruments your source and test files
 * to ${build.directory}/clover/src-instrumented and test-src-instrumented respectively. These directories are then set as the
 * project's source and test source directories that subsequently get compiled by the compiler MOJO.</p>
 *
 * @goal setup
 * @phase process-sources
 */
public class CloverSetupMojo extends CloverInstrumentInternalMojo {

    static Date START_DATE; 

    public void execute() throws MojoExecutionException {
        // store the start time of the build. ie - the very first compilation with clover.
        final MavenProject firstProject = getReactorProjects().get(0);
        if (firstProject == getProject()) {
            START_DATE = new Date();
        }
        super.execute();
    }


    protected String getSrcName() {
        return super.getSrcName() + "-instrumented";
    }

    protected String getSrcTestName() {
        return super.getSrcTestName() + "-instrumented";
    }

    protected void redirectOutputDirectories() {

    }

    protected void redirectArtifact() {

    }
}
