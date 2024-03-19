package org.openclover.maven.plugin.internal.lifecycle;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.openclover.runtime.api.CloverException;

import java.util.Set;

public class Maven3LifecycleAnalyzer extends MavenLifecycleAnalyzer {

    public Maven3LifecycleAnalyzer(@NotNull final LifecycleExecutor lifecycleExecutor,
                                   @NotNull final MavenProject mavenProject,
                                   @NotNull final MavenSession mavenSession) {
        super(lifecycleExecutor, mavenProject, mavenSession);
    }

    @Override
    public Set<String> getPhasesToBeExecuted() throws CloverException {
        try {
            final String[] tasks = mavenSession.getGoals().toArray(new String[0]);
            final MavenExecutionPlan plan = lifecycleExecutor.calculateExecutionPlan(mavenSession, tasks);
            return getPhasesFromMojoExecutions(plan.getMojoExecutions());
        } catch (Exception ex) {
            throw new CloverException(ex);
        }
    }

}
