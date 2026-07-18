package org.openclover.maven.plugin.internal.instrumentation;

import org.apache.maven.project.MavenProject;

import java.lang.reflect.Method;

/**
 * Helper for calling {@link MavenProject} methods that are not available on all supported Maven versions.
 * <p>
 * Maven 4.0.0 forbids modifying the compile source roots through the list returned by
 * {@code getCompileSourceRoots()} (or {@code getTestCompileSourceRoots()}) - doing so via
 * {@code iterator.remove()} triggers a deprecation warning and stops working. Instead, it exposes dedicated
 * {@code removeCompileSourceRoot(String)} / {@code removeTestCompileSourceRoot(String)} methods.
 * <p>
 * Since the plugin is compiled against the Maven 3.x API (which does not declare those methods), we invoke them
 * reflectively when running under Maven 4 and fall back to plain list mutation when running under Maven 3.
 */
final class MavenProjectCompat {

    private MavenProjectCompat() {
    }

    /**
     * Removes a compile source root from the project, using the Maven 4 API when available.
     *
     * @param project    the Maven project
     * @param sourceRoot the source root to remove
     */
    static void removeCompileSourceRoot(final MavenProject project, final String sourceRoot) {
        if (!invokeRemove(project, "removeCompileSourceRoot", sourceRoot)) {
            project.getCompileSourceRoots().remove(sourceRoot);
        }
    }

    /**
     * Removes a test compile source root from the project, using the Maven 4 API when available.
     *
     * @param project    the Maven project
     * @param sourceRoot the test source root to remove
     */
    static void removeTestCompileSourceRoot(final MavenProject project, final String sourceRoot) {
        if (!invokeRemove(project, "removeTestCompileSourceRoot", sourceRoot)) {
            project.getTestCompileSourceRoots().remove(sourceRoot);
        }
    }

    /**
     * @return {@code true} if the Maven 4 method existed and was invoked, {@code false} otherwise
     */
    private static boolean invokeRemove(final MavenProject project, final String methodName, final String sourceRoot) {
        try {
            final Method method = project.getClass().getMethod(methodName, String.class);
            method.invoke(project, sourceRoot);
            return true;
        } catch (NoSuchMethodException e) {
            // Maven 3.x - method not available, caller falls back to list mutation
            return false;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke MavenProject." + methodName, e);
        }
    }
}
