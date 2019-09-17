package com.atlassian.maven.plugins.sample;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Fork a build life cycle and run till the 'install' phase.
 */
@Execute(phase = LifecyclePhase.INSTALL, goal = "fork", lifecycle = "fork")
@Mojo(name = "fork")
public class LifecycleForkMojo extends AbstractMojo {
    public void execute() {

    }
}
