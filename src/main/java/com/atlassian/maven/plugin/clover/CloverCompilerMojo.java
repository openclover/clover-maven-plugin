package com.atlassian.maven.plugin.clover;

/**
 * This mojo is to be used mainly for incremental instrumentation and compilation of Java source code.
 *
 * NB: This does not run in a forked lifecycle.
 *
 * @goal setup
 */
public class CloverCompilerMojo extends CloverInstrumentInternalMojo {

    protected void redirectOutputDirectories() {

    }

    protected void redirectArtifact() {
        
    }
}
