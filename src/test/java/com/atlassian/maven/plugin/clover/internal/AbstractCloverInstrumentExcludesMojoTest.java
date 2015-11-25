package com.atlassian.maven.plugin.clover.internal;

import com.atlassian.maven.plugin.clover.CloverInstrumentMojo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link AbstractCloverInstrumentMojo#getExcludes()}
 */
@RunWith(Parameterized.class)
public class AbstractCloverInstrumentExcludesMojoTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new String[]{"file1", "file2"}, new String[]{"file1", "file2"}},
                {new String[]{"file1", "file1"}, new String[]{"file1"}},
                {new String[]{""}, new String[]{""}},
                {null, new String[]{}},
        });
    }

    private String excludesFile;
    private Set<String> expectedExcludes;

    public AbstractCloverInstrumentExcludesMojoTest(String[] linesInExcludesFile, String[] expectedExcludes) throws IOException {
        this.excludesFile = createTempFileWithLines(linesInExcludesFile);
        this.expectedExcludes = new HashSet<String>(Arrays.asList(expectedExcludes));
    }

    private String createTempFileWithLines(String[] lines) throws IOException {
        if (null != lines) {
            File temp = File.createTempFile("temp-file-name", ".tmp");
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
        excludesFileField.set(mojo, excludesFile);
        Set<String> excludes = mojo.getExcludes();
        assertEquals(expectedExcludes, excludes);
    }
}
