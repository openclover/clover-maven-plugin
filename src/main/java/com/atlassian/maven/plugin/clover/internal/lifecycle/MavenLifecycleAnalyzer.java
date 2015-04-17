package com.atlassian.maven.plugin.clover.internal.lifecycle;

import com.atlassian.clover.api.CloverException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public abstract class MavenLifecycleAnalyzer {

    protected final LifecycleExecutor lifecycleExecutor;
    protected final MavenProject mavenProject;
    protected final MavenSession mavenSession;

    public MavenLifecycleAnalyzer(@NotNull final LifecycleExecutor lifecycleExecutor,
                                  @NotNull final MavenProject mavenProject,
                                  @NotNull final MavenSession mavenSession) {
        this.lifecycleExecutor = lifecycleExecutor;
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
    }

    /**
     * Return <code>true</code> if current Maven version is compatible with this analyzer.
     *
     * @return boolean
     */
    public abstract boolean isCompatibleVersion();

    /**
     * Return list of build phases which will be executed. It may also return goals passed from a command line.
     *
     * @return Set&lt;String&gt;
     * @throws CloverException in case when build analysis has failed
     */
    public abstract Set<String> getPhasesToBeExecuted() throws CloverException;

    protected Set<String> getPhasesFromMojoExecutions(@NotNull final List<MojoExecution> mojoExecutions) {
        final Set<String> phases = Sets.newHashSet();
        for (final MojoExecution mojoExecution : mojoExecutions) {
            phases.addAll(getPhasesFromMojoExecution(mojoExecution));
        }
        return phases;
    }

    protected List<String> getPhasesFromMojoExecution(@NotNull final MojoExecution mojoExecution) {
        final List<String> phases = Lists.newArrayList();
        final String defaultPhase = mojoExecution.getMojoDescriptor().getPhase();
        if (defaultPhase != null) {
            phases.add(defaultPhase);
        }
        final String forkedPhase = mojoExecution.getMojoDescriptor().getExecutePhase();
        if (forkedPhase != null) {
            phases.add(forkedPhase);
        }
        return phases;
    }
}
