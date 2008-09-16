package com.atlassian.maven.plugin.clover;

/**
 * This mojo is to be used mainly for incremental instrumentation and compilation of Java source code.
 *
 * NB: This does not, nor should it, run in a forked lifecycle.
 *
 * @goal setup
 * @phase process-sources 
 */
public class CloverCompilerMojo extends CloverInstrumentInternalMojo {

    protected void redirectOutputDirectories() {

    }

    protected void redirectArtifact() {
        
    }
}
