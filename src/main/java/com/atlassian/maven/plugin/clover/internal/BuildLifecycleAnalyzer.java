package com.atlassian.maven.plugin.clover.internal;

import clover.com.google.common.collect.Lists;
import com.atlassian.clover.util.ReflectionUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        final String FAILED_POLLUTION_PROTECTION =
                "CLOVER: Failed to call Maven's internals via reflections, possibly this Maven version is "
                + "incompatible with Clover. Maven's build lifecycle could not be analyzed. Repository "
                + "pollution protection will not run. ";
        try {
            if (isMaven2()) {
                return findGoalsToBeExecutedInMaven2();
            } else if (isMaven3()) {
                return findGoalsToBeExecutedInMaven3();
            } else {
                log.warn(FAILED_POLLUTION_PROTECTION);
                return Collections.emptyList();
            }
        } catch (Exception ex) {
            log.warn(FAILED_POLLUTION_PROTECTION);
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
        try {
            // method present since Maven 3
            lifecycleExecutor.getClass().getDeclaredMethod("calculateExecutionPlan", MavenSession.class, String[].class);
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

                // TODO check which one returns better results, keep one of them
                allGoalsForTask = getPhasesFromLifecycleMappings(lifecycleMappings);
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

    @NotNull
    protected List<String> findGoalsToBeExecutedInMaven3()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // Using reflections as the following classes/methods are not available in Maven 2
        // MavenExecutionPlan plan = lifecycleExecutor.calculateExecutionPlan(...)
        final String[] tasks = ((List<String>) mavenSession.getGoals()).toArray(new String[0]);
        final Object plan = lifecycleExecutor_calculateExecutionPlan(lifecycleExecutor, mavenSession, tasks);
        return getPhasesFromMojoExecutions(mavenExecutionPlan_getMojoExecutions(plan));
    }

    private List<String> getPhasesFromLifecycleMappings(@NotNull final Map<String, List<MojoExecution>> lifecycleMappings) {
        final List<String> phases = Lists.newArrayList();
        for (final Map.Entry<String, List<MojoExecution>> mapping : lifecycleMappings.entrySet()) {
            phases.addAll(getPhasesFromMojoExecutions(mapping.getValue()));
        }
        return phases;
    }

    private List<String> getPhasesFromMojoExecutions(@NotNull final List<MojoExecution> mojoExecutions) {
        final List<String> phases = Lists.newArrayList();
        for (final MojoExecution mojoExecution : mojoExecutions) {
            phases.addAll(getPhasesFromMojoExecution(mojoExecution));
        }
        return phases;
    }

    private List<String> getPhasesFromMojoExecution(@NotNull final MojoExecution mojoExecution) {
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

    //////////////////////////////////////////////////////
    // Calling Maven 2 and Maven 3 methods via reflections
    //////////////////////////////////////////////////////

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

    //    private List<String> getGoalsFromProcessGoalChain(String task,
//                                                      Map<String, List<MojoExecution>> lifecycleMappings,
//                                                      Lifecycle lifecycle) {
//        final List<MojoExecution> mojoGoals = lifecycleExecutor_processGoalChain(lifecycleExecutor,
//                task, lifecycleMappings, lifecycle);
//        return Lists.transform(mojoGoals, new Function<MojoExecution, String>() {
//            @Override
//            public String apply(MojoExecution mojoExecution) {
//                return mojoExecution.getMojoDescriptor().getPhase();
//            }
//        });

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

    /**
     * Maven 2
     */
    private Map<String, Lifecycle> lifecycleExecutor_getPhaseToLifecycleMap(
            @NotNull final LifecycleExecutor lifecycleExecutor)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // return lifecycleExecutor.getPhaseToLifecycleMap()
        return (Map<String, Lifecycle>) ReflectionUtils.invokeVirtualImplicit("getPhaseToLifecycleMap", lifecycleExecutor);
    }

//    private List<MojoExecution> lifecycleExecutor_processGoalChain(
//            @NotNull final LifecycleExecutor lifecycleExecutor,
//            String task, Map<String, List<MojoExecution>> lifecycleMappings, Lifecycle lifecycle) {
//        try {
//            return (List<MojoExecution>) ReflectionUtils.invokeVirtualImplicit("processGoalChain", lifecycleExecutor,
//                    task, lifecycleMappings, lifecycle);
//        } catch (Exception e) {
//            return null;
//        }
//    }


}
