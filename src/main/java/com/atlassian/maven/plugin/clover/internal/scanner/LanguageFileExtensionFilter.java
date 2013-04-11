package com.atlassian.maven.plugin.clover.internal.scanner;

import com.atlassian.clover.spi.lang.Language;

import java.util.ArrayList;
import java.util.Set;

/**
 * Performs filtering of file list based on file extension(s) for given programming language.
 */
public abstract class LanguageFileExtensionFilter implements LanguageFileFilter {

    public abstract String[] filter(String[] inputFiles);

    protected String[] filter(final String[] inputFiles, final Set/*<String>*/ fileExtensions) {
        if (inputFiles == null) {
            return null;
        }

        final ArrayList/*<String>*/ filteredFiles = new ArrayList/*<String>*/(inputFiles.length);
        // copy files matching the extension(s)
        for (int i = 0; i < inputFiles.length; i++) {
            final int lastDotIndex = inputFiles[i].lastIndexOf('.');
            if (lastDotIndex != -1) {
                final String fileExt = inputFiles[i].substring(lastDotIndex); // get extension
                if (fileExtensions.contains(fileExt)) {
                    filteredFiles.add(inputFiles[i]);
                }
            }
        }
        return (String[]) filteredFiles.toArray(new String[0]);
    }

    /**
     * Filter accepting all sources.
     */
    public static final LanguageFileFilter ANY_LANGUAGE = new LanguageFileExtensionFilter() {
        public String[] filter(final String[] inputFiles) {
            return inputFiles;
        }

        @Override
        public String toString() {
            return "ANY_LANGUAGE";
        }
    };

    /**
     * Filter accepting Java sources only.
     *
     * @see com.atlassian.clover.spi.lang.Language.Builtin#JAVA
     */
    public static final LanguageFileFilter JAVA_LANGUAGE = new LanguageFileExtensionFilter() {
        @Override
        public String[] filter(final String[] inputFiles) {
            return filter(inputFiles, Language.Builtin.JAVA.getFileExtensions());
        }

        @Override
        public String toString() {
            return "JAVA_LANGUAGE";
        }
    };

    /**
     * Filter accepting Groovy sources only.
     *
     * @see Language.Builtin#GROOVY
     */
    public static final LanguageFileFilter GROOVY_LANGUAGE = new LanguageFileExtensionFilter() {
        public String[] filter(final String[] inputFiles) {
            return filter(inputFiles, Language.Builtin.GROOVY.getFileExtensions());
        }

        @Override
        public String toString() {
            return "GROOVY_LANGUAGE";
        }
    };
}
