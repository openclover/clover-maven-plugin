package com.atlassian.maven.plugin.clover.internal;

import org.apache.maven.project.MavenProject;

import java.io.File;

import com.atlassian.clover.CloverNames;

/**
 * A helper class to manage configuration of the clover-maven-plugin
 */
public class ConfigUtil {

    final CloverConfiguration config;

    public ConfigUtil(final CloverConfiguration config) {
        this.config = config;
    }

    public String resolveCloverDatabase() {
        if (config.getCloverDatabase() != null) {// allow cloverDatabase to be overwritten.
            return config.getCloverDatabase();
        }
        final MavenProject project = resolveMavenProject();
        return project.getBuild().getDirectory() + "/clover/clover.db";
    }

    /**
     * If a singleCloverDatabase is configured, return the first project in the reactor.
     * Otherwise, return the current project.
     * @return the project to use
     */
    public MavenProject resolveMavenProject() {
        // if a singleCloverDatabase should be used, use the execution root build dir
        return config.isSingleCloverDatabase() ? config.getReactorProjects().get(0) : config.getProject();
    }


    public File resolveSnapshotFile(final File snapshot) {

        if (snapshot != null) {
            return snapshot;
        }
        // unless specified, save the snapshot file to the basedir
        final MavenProject project = resolveMavenProject();
        final File baseCloverDir = new File(project.getBasedir(), ".clover");
        baseCloverDir.mkdir();
        return new File(baseCloverDir,  "clover" + CloverNames.SNAPSHOT_SUFFIX);

    }
}
