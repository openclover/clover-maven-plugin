package org.openclover.maven.plugin;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * <p><b>Attention: this goal requires Maven 3 and fails on Maven 4</b> - see the
 * <code>instrument</code> goal for details. On Maven 4 use the <code>setup</code> goal instead.</p>
 *
 * <p>This goal behaves exactly like the instrument goal, however when forking the lifecycle - it runs only to the 'test'
 * phase instead of all the way to the 'install' phase.</p>
 * <p>This goal should be used as an optimization - i.e. if the phases after 'test' take a very long time to run.</p>
 * <p>Instrument all sources using Clover and forks a custom lifecycle to execute project's tests on the instrumented code
 * so that a Clover database is created.</p>
 */
@Execute(phase = LifecyclePhase.TEST, goal = "instrument-test", lifecycle = "clover")
@Mojo(name = "instrument-test")
public class CloverInstrumentTestMojo extends CloverInstrumentMojo {
}