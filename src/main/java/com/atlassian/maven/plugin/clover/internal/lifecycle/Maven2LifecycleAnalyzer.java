package com.atlassian.maven.plugin.clover.internal.lifecycle;

import clover.com.google.common.collect.Lists;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.util.ReflectionUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class Maven2LifecycleAnalyzer extends MavenLifecycleAnalyzer {


    public Maven2LifecycleAnalyzer(@NotNull final LifecycleExecutor lifecycleExecutor,
                                   @NotNull final MavenProject mavenProject,
                                   @NotNull final MavenSession mavenSession) {
        super(lifecycleExecutor, mavenProject, mavenSession);
    }

    @Override
    public boolean isCompatibleVersion() {
        return isMaven2();
    }

    @Override
    public List<String> getPhasesToBeExecuted() throws CloverException {
        try {
            return findGoalsToBeExecutedInMaven2();
        } catch (NoSuchMethodException ex) {
            throw new CloverException(ex);
        } catch (InvocationTargetException ex) {
            throw new CloverException(ex);
        } catch (IllegalAccessException ex) {
            throw new CloverException(ex);
        }
    }

    protected boolean isMaven2() {
        try {
            // test that all methods we need to call are avialable
            lifecycleExecutor.getClass().getMethod("getPhaseToLifecycleMap");
            lifecycleExecutor.getClass().getDeclaredMethod("getLifecycleForPhase",
                    String.class);
            lifecycleExecutor.getClass().getDeclaredMethod("constructLifecycleMappings",
                    MavenSession.class, String.class, MavenProject.class, Lifecycle.class);
            lifecycleExecutor.getClass().getDeclaredMethod("processGoalChain",
                    String.class, Map.class, Lifecycle.class);
            // passed, seems to be Maven 2
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @NotNull
    protected List<String> findGoalsToBeExecutedInMaven2()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final List<String> allGoalsForAllTasks = Lists.newArrayList();

        // Based on analysis of Maven's DefaultLifecycleExecutor
        // mavenSession.getGoals() returns list of goals/phases defined in command line, which are called tasks
        for (Object taskObj : mavenSession.getGoals()) {
            final String task = taskObj.toString();
            final List<String> allGoalsForTask;
            // every task may be a build phase
            if (lifecycleExecutor_getPhaseToLifecycleMap(lifecycleExecutor).containsKey(task)) {
                // in such case find it's build life cycle and all goals required to run
                Lifecycle lifecycle = lifecycleExecutor_getLifecycleForPhase(lifecycleExecutor, task);
                Map<String, List<MojoExecution>> lifecycleMappings = lifecycleExecutor_constructLifecycleMappings(
                        lifecycleExecutor, mavenSession, task, mavenProject, lifecycle);

                allGoalsForTask = getPhasesFromLifecycleMappings(lifecycleMappings);
            } else {
                // ... or is just a single goal; in such case, there's no need to find lifecycle
                allGoalsForTask = Lists.newArrayList(task);
            }

            // collect all goals
            allGoalsForAllTasks.addAll(allGoalsForTask);
        }

        return allGoalsForAllTasks;
    }

    private List<String> getPhasesFromLifecycleMappings(@NotNull final Map<String, List<MojoExecution>> lifecycleMappings) {
        final List<String> phases = Lists.newArrayList();
        for (final Map.Entry<String, List<MojoExecution>> mapping : lifecycleMappings.entrySet()) {
            phases.addAll(getPhasesFromMojoExecutions(mapping.getValue()));
        }
        return phases;
    }

    // calling methods via reflections as Maven 2 and Maven 3 have different APIs

    private Map<String, Lifecycle> lifecycleExecutor_getPhaseToLifecycleMap(
            @NotNull final LifecycleExecutor lifecycleExecutor)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // return lifecycleExecutor.getPhaseToLifecycleMap()
        return (Map<String, Lifecycle>) ReflectionUtils.invokeVirtualImplicit("getPhaseToLifecycleMap", lifecycleExecutor);
    }

    private Map<String, List<MojoExecution>> lifecycleExecutor_constructLifecycleMappings(
            @NotNull final LifecycleExecutor lifecycleExecutor,
            MavenSession mavenSession, String task, MavenProject mavenProject, Lifecycle lifecycle)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // return lifecycleExecutor.constructLifecycleMappings(...)
        return (Map<String, List<MojoExecution>>) ReflectionUtils.invokeVirtualImplicit(
                "constructLifecycleMappings", lifecycleExecutor,
                mavenSession, task, mavenProject, lifecycle);
    }

    private Lifecycle lifecycleExecutor_getLifecycleForPhase(
            @NotNull final LifecycleExecutor lifecycleExecutor,
            String task)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // return lifecycleExecutor.getLifecycleForPhase(task)
        return (Lifecycle) ReflectionUtils.invokeVirtualImplicit("getLifecycleForPhase", lifecycleExecutor, task);
    }
}
