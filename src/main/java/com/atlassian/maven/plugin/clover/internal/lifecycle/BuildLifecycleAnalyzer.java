package com.atlassian.maven.plugin.clover.internal.lifecycle;

import com.atlassian.clover.api.CloverException;
import com.google.common.collect.Sets;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class BuildLifecycleAnalyzer {

    private final Log log;
    private final LifecycleExecutor lifecycleExecutor;
    private final MavenProject mavenProject;
    private final MavenSession mavenSession;
    private final Set<String> phases;

    public BuildLifecycleAnalyzer(@NotNull final Log log,
                                  @NotNull final LifecycleExecutor lifecycleExecutor,
                                  @NotNull final MavenProject mavenProject,
                                  @NotNull final MavenSession mavenSession) {
        this.log = log;
        this.lifecycleExecutor = lifecycleExecutor;
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.phases = getPhasesToBeExecuted();

        log.debug("CLOVER: " + getClass().getSimpleName() + " found following build phases:");
        for (String phase : Sets.newTreeSet(phases)) {
            log.debug("CLOVER: " + phase);
        }
    }

    public boolean isInstallPresent() {
        return phases.contains("install");
    }

    public boolean isDeployPresent() {
        return phases.contains("deploy");
    }

    @NotNull
    protected Set<String> getPhasesToBeExecuted() {
        final String FAILED_POLLUTION_PROTECTION =
                "CLOVER: Failed to call Maven's internals via reflections, possibly this Maven version is "
                        + "incompatible with Clover. Maven's build lifecycle could not be analyzed. Repository "
                        + "pollution protection will not run. ";
        try {
            Maven2LifecycleAnalyzer maven2Analyzer = new Maven2LifecycleAnalyzer(lifecycleExecutor, mavenProject, mavenSession);
            Maven3LifecycleAnalyzer maven3Analyzer = new Maven3LifecycleAnalyzer(lifecycleExecutor, mavenProject, mavenSession);

            if (maven2Analyzer.isCompatibleVersion()) {
                return maven2Analyzer.getPhasesToBeExecuted();
            } else if (maven3Analyzer.isCompatibleVersion()) {
                return maven3Analyzer.getPhasesToBeExecuted();
            } else {
                log.warn(FAILED_POLLUTION_PROTECTION);
                return Collections.emptySet();
            }
        } catch (CloverException ex) {
            log.warn(FAILED_POLLUTION_PROTECTION);
            return Collections.emptySet();
        }
    }

}
