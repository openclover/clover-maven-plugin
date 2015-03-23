package com.atlassian.maven.plugins.sample;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Fork a build life cycle and run till the 'install' phase.
 *
 * @goal fork
 * @execute phase="install" lifecycle="fork"
 */
public class LifecycleForkMojo extends AbstractMojo {
    public void execute() throws MojoExecutionException {

    }
}
