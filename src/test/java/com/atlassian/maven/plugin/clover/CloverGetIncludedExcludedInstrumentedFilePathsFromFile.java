package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.CloverInstrumentMojo;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverInstrumentMojo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link AbstractCloverInstrumentMojo#getExcludes()} and {@link AbstractCloverInstrumentMojo#getIncludes()} ()}
 */
@RunWith(Parameterized.class)
public class CloverGetIncludedExcludedInstrumentedFilePathsFromFile {

    /**
     * Test data for parametrized test run
     * @return Collection of arrays where,
     * - first element - list of file paths
     * - second element - expected file paths excluded from instrumentation
     * - third element - expected file paths included included in instrumentation
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new String[]{"file1", "file2"}, new String[]{"file1", "file2"}, new String[]{"file1", "file2"}},
                {new String[]{"file1", "file1"}, new String[]{"file1"}, new String[]{"file1"}},
                {new String[]{""}, new String[]{""}, new String[]{""}},
                {null, new String[]{}, new String[]{"**/*.java", "**/*.groovy"}},
        });
    }

    private String file;
    private Set<String> expectedExcludes;
    private Set<String> expectedIncludes;

    public CloverGetIncludedExcludedInstrumentedFilePathsFromFile(String[] linesInExcludesFile, String[] expectedExcludes, String[] expectedIncludes) throws IOException {
        this.file = createTempFileWithLines(linesInExcludesFile);
        this.expectedExcludes = new HashSet<String>(Arrays.asList(expectedExcludes));
        this.expectedIncludes = new HashSet<String>(Arrays.asList(expectedIncludes));
    }

    private String createTempFileWithLines(String[] lines) throws IOException {
        if (null != lines) {
            File temp = File.createTempFile("includesExcludes", ".tmp");
            FileOutputStream fos = new FileOutputStream(temp);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
            bw.close();
            return temp.getAbsolutePath();
        } else {
            return null;
        }
    }

    @Test
    public void testReturnsExcludesFromSpecifiedExcludesFile() throws NoSuchFieldException, IllegalAccessException {
        CloverInstrumentMojo mojo = new CloverInstrumentMojo();
        Field excludesFileField = AbstractCloverInstrumentMojo.class.getDeclaredField("excludesFile");
        excludesFileField.setAccessible(true);
        excludesFileField.set(mojo, file);
        Set<String> excludes = mojo.getExcludes();
        assertEquals(expectedExcludes, excludes);
    }

    @Test
    public void testReturnsIncludesFromSpecifiedIncludesFile() throws NoSuchFieldException, IllegalAccessException {
        CloverInstrumentMojo mojo = new CloverInstrumentMojo();
        Field includesFileField = AbstractCloverInstrumentMojo.class.getDeclaredField("includesFile");
        includesFileField.setAccessible(true);
        includesFileField.set(mojo, file);
        Set<String> includes = mojo.getIncludes();
        assertEquals(expectedIncludes, includes);
    }
}
