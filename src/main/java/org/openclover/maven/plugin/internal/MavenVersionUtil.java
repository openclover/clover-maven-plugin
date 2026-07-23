package org.openclover.maven.plugin.internal;

import org.apache.maven.execution.MavenSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detection of the Maven version the plugin currently runs on. Maven publishes its own version as the
 * <code>maven.version</code> system property (Maven 3.x and Maven 4.x alike), so no extra component is needed.
 */
public class MavenVersionUtil {

    private MavenVersionUtil() {
    }

    /**
     * @param mavenSession current build session
     * @return version of the running Maven, e.g. "3.9.9" or "4.0.0-rc-5", or <code>null</code> if unknown
     */
    @Nullable
    public static String getMavenVersion(@NotNull final MavenSession mavenSession) {
        String version = mavenSession.getSystemProperties().getProperty("maven.version");
        if (version == null) {
            version = System.getProperty("maven.version");
        }
        return version;
    }

    /**
     * @param mavenSession current build session
     * @return true if running on Maven 4.0 or newer; false for Maven 3.x or when the version cannot be determined
     */
    public static boolean isMaven4OrLater(@NotNull final MavenSession mavenSession) {
        return isMaven4OrLater(getMavenVersion(mavenSession));
    }

    /**
     * @param version Maven version string, may be <code>null</code>
     * @return true if the major version number is 4 or greater
     */
    public static boolean isMaven4OrLater(@Nullable final String version) {
        if (version == null) {
            return false;
        }
        final int dot = version.indexOf('.');
        final String major = dot > 0 ? version.substring(0, dot) : version;
        try {
            return Integer.parseInt(major.trim()) >= 4;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
