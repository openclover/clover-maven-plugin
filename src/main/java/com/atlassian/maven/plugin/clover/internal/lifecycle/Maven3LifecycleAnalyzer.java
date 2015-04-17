package com.atlassian.maven.plugin.clover.internal.lifecycle;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.util.ReflectionUtils;
import com.google.common.collect.Iterables;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class Maven3LifecycleAnalyzer extends MavenLifecycleAnalyzer {

    public Maven3LifecycleAnalyzer(@NotNull final LifecycleExecutor lifecycleExecutor,
                                   @NotNull final MavenProject mavenProject,
                                   @NotNull final MavenSession mavenSession) {
        super(lifecycleExecutor, mavenProject, mavenSession);
    }

    @Override
    public boolean isCompatibleVersion() {
        return isMaven3();
    }

    @Override
    public Set<String> getPhasesToBeExecuted() throws CloverException {
        try {
            return findGoalsToBeExecutedInMaven3();
        } catch (NoSuchMethodException ex) {
            throw new CloverException(ex);
        } catch (IllegalAccessException ex) {
            throw new CloverException(ex);
        } catch (InvocationTargetException ex) {
            throw new CloverException(ex);
        }
    }

    protected boolean isMaven3() {
        try {
            // method present since Maven 3
            lifecycleExecutor.getClass().getDeclaredMethod("calculateExecutionPlan", MavenSession.class, String[].class);
            // passed, seems to be Maven 3
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @NotNull
    protected Set<String> findGoalsToBeExecutedInMaven3()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // Using reflections as the following classes/methods are not available in Maven 2
        // MavenExecutionPlan plan = lifecycleExecutor.calculateExecutionPlan(...)
        final String[] tasks = Iterables.toArray((List<String>) mavenSession.getGoals(), String.class);
        final Object plan = lifecycleExecutor_calculateExecutionPlan(lifecycleExecutor, mavenSession, tasks);
        return getPhasesFromMojoExecutions(mavenExecutionPlan_getMojoExecutions(plan));
    }

    // calling methods via reflections as Maven 2 and Maven 3 have different APIs

    private Object/*MavenExecutionPlan*/ lifecycleExecutor_calculateExecutionPlan(
            @NotNull final LifecycleExecutor lifecycleExecutor,
            MavenSession mavenSession, String[] tasks)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // return lifecycleExecutor.calculateExecutionPlan(...)
        final Method calculateExecutionPlan = lifecycleExecutor.getClass().getDeclaredMethod(
                "calculateExecutionPlan", MavenSession.class, String[].class);
        return calculateExecutionPlan.invoke(lifecycleExecutor, mavenSession, tasks);
    }

    private List<MojoExecution> mavenExecutionPlan_getMojoExecutions(Object/*MavenExecutionPlan*/ plan)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // return mavenExecutionPlan.getMojoExecutions()
        return (List<MojoExecution>) ReflectionUtils.invokeVirtualImplicit("getMojoExecutions", plan);
    }

}
