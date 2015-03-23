package com.atlassian.maven.plugin.clover.internal;

import clover.com.google.common.base.Function;
import clover.com.google.common.collect.Lists;
import com.atlassian.clover.util.ReflectionUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BuildLifecycleAnalyzer {

    private final Log log;
    private final LifecycleExecutor lifecycleExecutor;
    private final MavenProject mavenProject;
    private final MavenSession mavenSession;

    public BuildLifecycleAnalyzer(@NotNull final Log log,
                                  @NotNull final LifecycleExecutor lifecycleExecutor,
                                  @NotNull final MavenProject mavenProject,
                                  @NotNull final MavenSession mavenSession) {
        this.log = log;
        this.lifecycleExecutor = lifecycleExecutor;
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
    }

    @NotNull
    protected List<String> findGoalsToBeExecuted() {
        if (isMaven2()) {
            return findGoalsToBeExecutedInMaven2();
        } else if (isMaven3()) {
            return findGoalsToBeExecutedInMaven3();
        } else {
            log.warn("CLOVER: Failed to call Maven's internals via reflections, possibly this Maven version is "
                    + "incompatible with Clover. Maven's build lifecycle could not be analyzed.");
            return Collections.emptyList();
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

    protected boolean isMaven3() {
        return false; // TODO
    }

    /**
     * Based on analysis of Maven's DefaultLifecycleExecutor
     */
    @NotNull
    protected List<String> findGoalsToBeExecutedInMaven2() {
        final List<String> allGoalsForAllTasks = Lists.newArrayList();

        // mavenSession.getGoals() returns list of goals/phases defined in command line, which are called tasks
        for (Object taskObj : mavenSession.getGoals()) {
            final String task = taskObj.toString();
            final List<String> allGoalsForTask;
            // every task may be a build phase
            if (reflection_getPhaseToLifecycleMap(lifecycleExecutor).containsKey(task)) {
                // in such case find it's build life cycle and all goals required to run
                Lifecycle lifecycle = reflection_getLifecycleForPhase(lifecycleExecutor, task);
                Map<String, List<MojoExecution>> lifecycleMappings = reflection_constructLifecycleMappings(
                        lifecycleExecutor, mavenSession, task, mavenProject, lifecycle);

                // TODO check which one returns better results, keep one of them
                allGoalsForTask = getGoalsFromLifecycleMappings(lifecycleMappings);
//                allGoalsForTask = getGoalsFromProcessGoalChain(task, lifecycleMappings, lifecycle);
            } else {
                // ... or is just a single goal; in such case, there's no need to find lifecycle
                allGoalsForTask = Lists.newArrayList(task);
            }

            // collect all goals
            allGoalsForAllTasks.addAll(allGoalsForTask);
        }

        return allGoalsForAllTasks;
    }

    private List<String> getGoalsFromLifecycleMappings(Map<String, List<MojoExecution>> lifecycleMappings) {
        List<String> phases = Lists.newArrayList();
        for (Map.Entry<String, List<MojoExecution>> mapping : lifecycleMappings.entrySet()) {
            for (MojoExecution mojoExecution : mapping.getValue()) {
                String defaultPhase = mojoExecution.getMojoDescriptor().getPhase();
                if (defaultPhase != null) {
                    phases.add(defaultPhase);
                }
                String forkedPhase = mojoExecution.getMojoDescriptor().getExecutePhase();
                if (forkedPhase != null) {
                    phases.add(forkedPhase);
                }
            }
        }
        return phases;
    }

    private List<String> getGoalsFromProcessGoalChain(String task,
                                                      Map<String, List<MojoExecution>> lifecycleMappings,
                                                      Lifecycle lifecycle) {
        final List<MojoExecution> mojoGoals = reflection_processGoalChain(lifecycleExecutor,
                task, lifecycleMappings, lifecycle);
        return Lists.transform(mojoGoals, new Function<MojoExecution, String>() {
            @Override
            public String apply(MojoExecution mojoExecution) {
                return mojoExecution.getMojoDescriptor().getPhase();
            }
        });
    }

    @NotNull
    protected List<String> findGoalsToBeExecutedInMaven3() {
        final List<String> allGoalsForAllTasks = Lists.newArrayList();

        // mavenSession.getGoals() returns list of goals/phases defined in command line, which are called tasks
        for (Object taskObj : mavenSession.getGoals()) {
            final String task = (String) taskObj;
            final List<String> allGoalsForTask;

            // phase or goal?
            if (false) {
                // build phase - find it's build life cycle and all phases required to run
                allGoalsForTask = Lists.newArrayList(task); // TODO HANDLE MAVEN 3
            } else {
                // or just a single goal
                allGoalsForTask = Lists.newArrayList(task);
            }
            // collect all goals
            allGoalsForAllTasks.addAll(allGoalsForTask);
        }
        return allGoalsForAllTasks;
    }

    private Map<String, List<MojoExecution>> reflection_constructLifecycleMappings(
            @NotNull final LifecycleExecutor lifecycleExecutor,
            MavenSession mavenSession, String task, MavenProject mavenProject, Lifecycle lifecycle) {
        try {
            return (Map<String, List<MojoExecution>>) ReflectionUtils.invokeVirtualImplicit(
                    "constructLifecycleMappings", lifecycleExecutor,
                    mavenSession, task, mavenProject, lifecycle);
        } catch (Exception e) {
            return null;
        }
    }

    private Lifecycle reflection_getLifecycleForPhase(@NotNull final LifecycleExecutor lifecycleExecutor, String task) {
        try {
            return (Lifecycle) ReflectionUtils.invokeVirtualImplicit("getLifecycleForPhase", lifecycleExecutor, task);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Lifecycle> reflection_getPhaseToLifecycleMap(@NotNull final LifecycleExecutor lifecycleExecutor) {
        try {
            return (Map<String, Lifecycle>) ReflectionUtils.invokeVirtualImplicit("getPhaseToLifecycleMap", lifecycleExecutor);
        } catch (Exception e) {
            return null;
        }
    }

    private List<MojoExecution> reflection_processGoalChain(
            @NotNull final LifecycleExecutor lifecycleExecutor,
            String task, Map<String, List<MojoExecution>> lifecycleMappings, Lifecycle lifecycle) {
        try {
            return (List<MojoExecution>) ReflectionUtils.invokeVirtualImplicit("processGoalChain", lifecycleExecutor,
                    task, lifecycleMappings, lifecycle);
        } catch (Exception e) {
            return null;
        }
    }

}
