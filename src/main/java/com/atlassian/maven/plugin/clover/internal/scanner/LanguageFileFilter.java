package com.atlassian.maven.plugin.clover.internal.scanner;

/**
 * Performs filtering of file list based on the expected programming language.
 */
public interface LanguageFileFilter {

    /**
     * Filters out files not matching programming language from <code>inputFiles</code> and
     * returns result in new <code>String[]</code> array.
     *
     * @param inputFiles list of files to be filtered
     * @return String[] list of files matching programming language
     */
    String[] filter(String[] inputFiles);

}
