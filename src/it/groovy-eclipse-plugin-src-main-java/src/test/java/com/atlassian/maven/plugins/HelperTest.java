package com.atlassian.maven.plugins;

import junit.framework.TestCase;

/**
 * Tests for the {@link Helper} class.
 */
public class HelperTest extends TestCase {
    public void testHelp() {
        new Helper().help(new Example());
    }
}
