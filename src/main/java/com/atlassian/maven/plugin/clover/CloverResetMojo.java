package com.atlassian.maven.plugin.clover;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Reset source directories to original ones.
 */
@Mojo(name = "reset")
public class CloverResetMojo extends CloverInstrumentInternalMojo {

    @Override
    public void execute() {
        getLog().info("Resetting directories for artifact: " + getProject().getId());
        CloverInstrumentInternalMojo.resetSrcDirsOriginal(getProject().getArtifact(), this);
    }
}
