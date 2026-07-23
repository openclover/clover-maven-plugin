package org.openclover.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

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
 * <p>This is the recommended (and, on Maven 4, the only) way to instrument sources - the <code>instrument</code>
 * goal and its forked lifecycle are supported on Maven 3 only.</p>
 *
 * <p><b>Note: since version 5.1.0 the repository pollution protection is enabled by default</b>
 * (<code>maven.clover.repositoryPollutionProtection</code> now defaults to <code>true</code>). Because this goal
 * instruments sources in the main lifecycle, running the <code>install</code> or <code>deploy</code> phase would
 * put instrumented classes into your local cache or a binaries repository; such a build now fails with an
 * explanatory message. Run the build up to the <code>verify</code> phase, or set the flag to <code>false</code>
 * if the installation of instrumented artifacts is intentional.</p>
 */
@Mojo(name = "setup", defaultPhase = LifecyclePhase.PROCESS_SOURCES,
        requiresDependencyResolution = ResolutionScope.TEST)
public class CloverSetupMojo extends CloverInstrumentInternalMojo {

    static Date START_DATE; 

    @Override
    public void execute() throws MojoExecutionException {
        // store the start time of the build. ie - the very first compilation with clover.
        final MavenProject firstProject = getReactorProjects().get(0);
        if (firstProject == getProject()) {
            START_DATE = new Date();
        }
        super.execute();
    }

    @Override
    protected boolean shouldRedirectArtifacts() {
        return false;
    }

    @Override
    protected boolean shouldRedirectOutputDirectories() {
        return false;
    }

    @Override
    protected String getSrcName() {
        return super.getSrcName() + "-instrumented";
    }

    @Override
    protected String getSrcTestName() {
        return super.getSrcTestName() + "-instrumented";
    }

}
