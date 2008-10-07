package com.atlassian.maven.plugin.clover.internal;

import org.apache.maven.project.MavenProject;

/**
 * A helper class to manage configuration of the maven-clover2-plugin
 */
public class ConfigUtil {

    final CloverConfiguration config;

    public ConfigUtil(CloverConfiguration config) {
        this.config = config;
    }

    public String resolveCloverDatabase() {
        if (config.getCloverDatabase() != null) {// allow cloverDatabase to be overwritten.
            return config.getCloverDatabase();
        }

        // if a singleCloverDatabase should be used, use the execution root build dir

        final MavenProject project = config.isSingleCloverDatabase() ?
                                        (MavenProject) config.getReactorProjects().get(0) :
                                        config.getProject();
        
        return project.getBuild().getDirectory() + "/clover/clover.db";
    }
}
