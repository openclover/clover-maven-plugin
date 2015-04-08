package com.atlassian.maven.plugin.clover;


import org.apache.maven.plugin.MojoExecutionException;

/**
 * <p>This goal behaves exactly like the instrument goal, however when forking the lifecycle - it runs only to the 'test'
 * phase instead of all the way to the 'install' phase.</p>
 * <p>This goal should be used as an optimization - ie. if the phases after 'test' take a very long time to run.</p>
 * <p>Instrument all sources using Clover and forks a custom lifecycle to execute project's tests on the instrumented code
 * so that a Clover database is created.</p>
 *
 * @goal instrument-test
 * @execute phase="test" lifecycle="clover"
 */
public class CloverInstrumentTestMojo extends CloverInstrumentMojo {
    /**
     * {@inheritDoc}
     *
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute() throws MojoExecutionException {
        super.execute();
    }
}