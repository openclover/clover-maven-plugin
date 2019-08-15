package com.atlassian.maven.plugin.clover.internal.lifecycle;

import com.atlassian.clover.api.CloverException;
import com.google.common.collect.Iterables;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class Maven3LifecycleAnalyzer extends MavenLifecycleAnalyzer {

    public Maven3LifecycleAnalyzer(@NotNull final LifecycleExecutor lifecycleExecutor,
                                   @NotNull final MavenProject mavenProject,
                                   @NotNull final MavenSession mavenSession) {
        super(lifecycleExecutor, mavenProject, mavenSession);
    }

    @Override
    public boolean isCompatibleVersion() {
        return true;
    }

    @Override
    public Set<String> getPhasesToBeExecuted() throws CloverException {
        try {
            final String[] tasks = Iterables.toArray(mavenSession.getGoals(), String.class);
            final MavenExecutionPlan plan = lifecycleExecutor.calculateExecutionPlan(mavenSession, tasks);
            return getPhasesFromMojoExecutions(plan.getMojoExecutions());
        } catch (Exception ex) {
            throw new CloverException(ex);
        }
    }

}
