package org.openclover.maven.plugin.internal;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MavenVersionUtilTest {

    @Test
    public void maven3VersionsAreNotMaven4() {
        assertFalse(MavenVersionUtil.isMaven4OrLater("3.3.1"));
        assertFalse(MavenVersionUtil.isMaven4OrLater("3.9.2"));
        assertFalse(MavenVersionUtil.isMaven4OrLater("3.9.11"));
    }

    @Test
    public void maven4VersionsAreDetected() {
        assertTrue(MavenVersionUtil.isMaven4OrLater("4.0.0"));
        assertTrue(MavenVersionUtil.isMaven4OrLater("4.0.0-rc-5"));
        assertTrue(MavenVersionUtil.isMaven4OrLater("4.1.0-SNAPSHOT"));
        assertTrue(MavenVersionUtil.isMaven4OrLater("5.0.0"));
    }

    @Test
    public void unknownVersionIsTreatedAsMaven3() {
        assertFalse(MavenVersionUtil.isMaven4OrLater((String) null));
        assertFalse(MavenVersionUtil.isMaven4OrLater(""));
        assertFalse(MavenVersionUtil.isMaven4OrLater("unknown"));
    }
}
