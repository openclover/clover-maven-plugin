package com.atlassian.maven.plugin.clover;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * 
 * @goal reset
 */
public class CloverResetMojo extends CloverInstrumentInternalMojo {
   
    public void execute() throws MojoExecutionException {
        getLog().info("Resetting directories for artifact: " + getProject().getId());
        CloverInstrumentInternalMojo.resetSrcDirsOriginal(getProject().getArtifact(), this);
    }
}
