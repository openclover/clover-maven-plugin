package com.atlassian.maven.plugin.clover.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * TODO: Document this class / interface here
 *
 * @since v7.0
 */
@RunWith(Parameterized.class)
public class AbstractCloverInstrumentExcludesMojoTest {

    class TestableCloverInstrumentExcludesMojo extends AbstractCloverInstrumentMojo {

        public TestableCloverInstrumentExcludesMojo(final String excludesFile) {
            this.excludesFile = excludesFile;
        }

        @Override
        protected boolean shouldRedirectArtifacts() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        protected boolean shouldRedirectOutputDirectories() {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

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
    public void testReturnsExcludesFromSpecifiedExcludesFile() {
        TestableCloverInstrumentExcludesMojo mojo = new TestableCloverInstrumentExcludesMojo(excludesFile);
        Set<String> excludes = mojo.getExcludes();
        assertEquals(expectedExcludes, excludes);
    }
}
