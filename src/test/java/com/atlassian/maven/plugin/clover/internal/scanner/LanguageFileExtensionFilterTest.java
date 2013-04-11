package com.atlassian.maven.plugin.clover.internal.scanner;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test for {@link LanguageFileExtensionFilter}
 */
public class LanguageFileExtensionFilterTest {
    static final String[] TEST_FILES = new String[] {
            "Foo.java",
            "Goo.groovy",
            "Hoo.other",
            ".",
            ".startWithDot",
            "noExtension"
    };

    static final String[] JAVA_TEST_FILES = new String[] {
            "Foo.java",
            "Goo.java",
            "Hoo.java"
    };

    @Test
    public void testAnyLanguageFilterNull() {
        assertNull(LanguageFileExtensionFilter.ANY_LANGUAGE.filter(null));
    }

    @Test
    public void testAnyLanguageFilterEmpty() {
        final String[] actual = LanguageFileExtensionFilter.ANY_LANGUAGE.filter(new String[0]);
        assertEquals(0, actual.length);
    }

    @Test
    public void testAnyLanguageFilterEverything() {
        final String[] actual = LanguageFileExtensionFilter.ANY_LANGUAGE.filter(TEST_FILES);
        assertArrayEquals(TEST_FILES, actual);
    }

    @Test
    public void testJavaLanguageFilterNull() {
        assertNull(LanguageFileExtensionFilter.JAVA_LANGUAGE.filter(null));
    }

    @Test
    public void testJavaLanguageFilterEmpty() {
        final String[] actual = LanguageFileExtensionFilter.JAVA_LANGUAGE.filter(new String[0]);
        assertEquals(0, actual.length);
    }

    @Test
    public void testJavaLanguageFilterEverything() {
        final String[] actual = LanguageFileExtensionFilter.JAVA_LANGUAGE.filter(JAVA_TEST_FILES);
        assertArrayEquals(JAVA_TEST_FILES, actual);
    }

    @Test
    public void testJavaLanguageFilterSomething() {
        final String[] actual = LanguageFileExtensionFilter.JAVA_LANGUAGE.filter(TEST_FILES);
        final String[] expected = new String[] { "Foo.java" };
        assertArrayEquals(expected, actual);
    }
}
