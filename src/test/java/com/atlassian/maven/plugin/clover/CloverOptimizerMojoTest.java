package com.atlassian.maven.plugin.clover;

import com.atlassian.clover.util.FileUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link CloverOptimizerMojo}
 */
public class CloverOptimizerMojoTest {

    /**
     * Helper class to define directory structure for tests.
     */
    protected class DirNode {
        boolean isFile;
        String name;
        DirNode children[];

        public DirNode(final String name) {
            this.name = name;
            children = new DirNode[0];
            isFile = true;
        }

        public DirNode(final String name, final DirNode ... children) {
            this.name = name;
            this.children = children;
            isFile = false;
        }
    }

    /** Mojo under test */
    CloverOptimizerMojo mojo = new CloverOptimizerMojo();

    /** Ant project stub */
    protected Project project;

    /** Layout of directory structure */
    protected DirNode testLayout;

    /** Helper map (file -> platfrom specific path) */
    protected Map<String, String> path;

    /** Location of root directory of test structure */
    protected File testDirRoot;



    /**
     * Prepare directory structure for tests.
     */
    @Before
    public void setUp() throws IOException {
        // our test directory layout
        testLayout = createLayout();
        path = createPathMap();

        // create temporary directory and make sure it's empty
        testDirRoot = createEmptyDirectory(getTempDirName());
        project = new Project();
        project.setBaseDir(testDirRoot);

        // create our test layout
        createDirectoryLayout(testDirRoot, testLayout);
    }

    /**
     * Test for {@link CloverOptimizerMojo#createFileSet(org.apache.tools.ant.Project, java.io.File, java.util.List, java.util.List)}
     * with empty includes / excludes elements. It should include all files from directory.
     */
    @Test
    public void testCreateFileSetEmptyIncludeNullExclude() {
        // input
        final List<String> includes = Collections.emptyList();

        // expected output
        final List<String> expectedIncludedFiles = new ArrayList<String>() {{
            add(path.get("MyFirstTest"));
            add(path.get("MySecondTest"));
            add(path.get("Foo"));
            add(path.get("Goo"));
            add(path.get("Hoo"));
        }};
        final List<String> expectedExcludedFiles = Collections.emptyList();

        // scan directory; note: excludes are optional and thus can be null
        final FileSet fileSet = mojo.createFileSet(project, testDirRoot, includes, null);
        final DirectoryScanner dirScanner = fileSet.getDirectoryScanner();
        final List<String> includedFiles = Arrays.asList(dirScanner.getIncludedFiles());
        final List<String> excludedFiles = Arrays.asList(dirScanner.getExcludedFiles());

        // check results
        assertListsEqual(expectedIncludedFiles, includedFiles);
        assertListsEqual(expectedExcludedFiles, excludedFiles);
    }


    /**
     * Test for {@link CloverOptimizerMojo#createFileSet(org.apache.tools.ant.Project, java.io.File, java.util.List, java.util.List)}
     * with empty includes / excludes elements. It should include all files from directory.
     */
    @Test
    public void testCreateFileSetEmptyIncludeExclude() {
        // input
        final List<String> includes = Collections.emptyList();
        final List<String> excludes = Collections.emptyList();

        // expected output
        final List<String> expectedIncludedFiles = new ArrayList<String>() {{
            add(path.get("MyFirstTest"));
            add(path.get("MySecondTest"));
            add(path.get("Foo"));
            add(path.get("Goo"));
            add(path.get("Hoo"));
        }};
        final List<String> expectedExcludedFiles = Collections.emptyList();

        // scan directory
        final FileSet fileSet = mojo.createFileSet(project, testDirRoot, includes, excludes);
        final DirectoryScanner dirScanner = fileSet.getDirectoryScanner();
        final List<String> includedFiles = Arrays.asList(dirScanner.getIncludedFiles());
        final List<String> excludedFiles = Arrays.asList(dirScanner.getExcludedFiles());

        // check results
        assertListsEqual(expectedIncludedFiles, includedFiles);
        assertListsEqual(expectedExcludedFiles, excludedFiles);
    }

    /**
     * Test for {@link CloverOptimizerMojo#createFileSet(org.apache.tools.ant.Project, java.io.File, java.util.List, java.util.List)}
     * having only one include / exclude expression.
     */
    @Test
    public void testCreateFileSetOneIncludeExclude() {
        // input
        final List<String> includes = new ArrayList<String>() {{
            add("**/test/*.java");
        }};
        final List<String> excludes = new ArrayList<String>() {{
            add("**/*Second*");
        }};

        // expected output
        final List<String> expectedIncludedFiles = new ArrayList<String>() {{    // includes
            add(path.get("MyFirstTest"));
        }};
        final List<String> expectedNotIncludedFiles = new ArrayList<String>() {{ // ! includes
            add(path.get("Foo"));
            add(path.get("Goo"));
            add(path.get("Hoo"));
        }};
        final List<String> expectedExcludedFiles = new ArrayList<String>() {{    // includes & excludes
            add(path.get("MySecondTest"));
        }};

        // scan directory
        final FileSet fileSet = mojo.createFileSet(project, testDirRoot, includes, excludes);
        final DirectoryScanner dirScanner = fileSet.getDirectoryScanner();
        final List<String> includedFiles = Arrays.asList(dirScanner.getIncludedFiles());
        final List<String> notIncludedFiles = Arrays.asList(dirScanner.getNotIncludedFiles());
        final List<String> excludedFiles = Arrays.asList(dirScanner.getExcludedFiles());

        // check results
        assertListsEqual(expectedIncludedFiles, includedFiles);
        assertListsEqual(expectedNotIncludedFiles, notIncludedFiles);
        assertListsEqual(expectedExcludedFiles, excludedFiles);
    }


    /**
     * Test for {@link CloverOptimizerMojo#createFileSet(org.apache.tools.ant.Project, java.io.File, java.util.List, java.util.List)}
     * with several includes / excludes elements on lists, where each element contains one path.
     */
    @Test
    public void testCreateFileSetMultipleIncludeExcludeOnePath() {
        // input
        final List<String> includes = new ArrayList<String>() {{
            add("src/test/*.java");
            add("src/main/**");
        }};
        final List<String> excludes = new ArrayList<String>() {{
            add("**/*MyFirst*");
            add("**/Foo*");
        }};

        // expected output
        final List<String> expectedIncludedFiles = new ArrayList<String>() {{
            add(path.get("MySecondTest"));
            add(path.get("Goo"));
            add(path.get("Hoo"));
        }};
        final List<String> expectedExcludedFiles = new ArrayList<String>() {{
            add(path.get("MyFirstTest"));
            add(path.get("Foo"));
        }};

        // scan directory
        final FileSet fileSet = mojo.createFileSet(project, testDirRoot, includes, excludes);
        final DirectoryScanner dirScanner = fileSet.getDirectoryScanner();
        final List<String> includedFiles = Arrays.asList(dirScanner.getIncludedFiles());
        final List<String> excludedFiles = Arrays.asList(dirScanner.getExcludedFiles());

        // check results
        assertListsEqual(expectedIncludedFiles, includedFiles);
        assertListsEqual(expectedExcludedFiles, excludedFiles);
    }

    /**
     * Test for {@link CloverOptimizerMojo#createFileSet(org.apache.tools.ant.Project, java.io.File, java.util.List, java.util.List)}
     * with several includes / excludes elements on lists, where each element contains multiple paths separated by comma.
     */
    @Test
    public void testCreateFileSetMultipleIncludeExcludeManyPaths() {
        // input
        final List<String> includes = new ArrayList<String>() {{
            add("**/My*, **/*oo*");
        }};
        final List<String> excludes = new ArrayList<String>() {{
            add("**/*First*, **/Goo*");
        }};

        // expected output
        final List<String> expectedIncludedFiles = new ArrayList<String>() {{
            add(path.get("MySecondTest"));
            add(path.get("Foo"));
            add(path.get("Hoo"));
        }};
        final List<String> expectedExcludedFiles = new ArrayList<String>() {{
            add(path.get("MyFirstTest"));
            add(path.get("Goo"));
        }};

        // scan directory
        final FileSet fileSet = mojo.createFileSet(project, testDirRoot, includes, excludes);
        final DirectoryScanner dirScanner = fileSet.getDirectoryScanner();
        final List<String> includedFiles = Arrays.asList(dirScanner.getIncludedFiles());
        final List<String> excludedFiles = Arrays.asList(dirScanner.getExcludedFiles());

        // check results
        assertListsEqual(expectedIncludedFiles, includedFiles);
        assertListsEqual(expectedExcludedFiles, excludedFiles);
    }

    /**
     * Test for {@link CloverOptimizerMojo#createFileSet(org.apache.tools.ant.Project, java.io.File, java.util.List, java.util.List)}
     * with several includes / excludes containing a regular expression. It should take all files from directory
     * matching a regular expression.
     */
    @Test
    public void testCreateFileSetIncludeExcludeRegExp() {
        // input
        final List<String> includes = new ArrayList<String>() {{
            add("%regex[.*(Test|oo).java]");
        }};
        final List<String> excludes = new ArrayList<String>() {{
            add("%regex[.*(Second|[GH]oo).*]");
        }};

        // expected output
        final List<String> expectedIncludedFiles = new ArrayList<String>() {{
            add(path.get("MyFirstTest"));
            add(path.get("Foo"));
        }};
        final List<String> expectedExcludedFiles = new ArrayList<String>() {{
            add(path.get("MySecondTest"));
            add(path.get("Goo"));
            add(path.get("Hoo"));
        }};

        // scan directory
        final FileSet fileSet = mojo.createFileSet(project, testDirRoot, includes, excludes);
        final DirectoryScanner dirScanner = fileSet.getDirectoryScanner();
        final List<String> includedFiles = Arrays.asList(dirScanner.getIncludedFiles());
        final List<String> excludedFiles = Arrays.asList(dirScanner.getExcludedFiles());

        // check results
        assertListsEqual(expectedIncludedFiles, includedFiles);
        assertListsEqual(expectedExcludedFiles, excludedFiles);
    }

    /**
     * Test for CloverOptimizerMojo#explodePaths()
     */
    @Test
    public void testExplodePathsEmptyList() {
        final List<String> inputEmpty = Collections.emptyList();
        final List<String> inputWhitespace = new ArrayList<String>() {{
            add("     ");
            add("");
        }};
        final List<String> empty = Collections.emptyList();

        assertListsEqual(empty, mojo.explodePaths(null, inputEmpty));
        assertListsEqual(empty, mojo.explodePaths(null, inputWhitespace));
    }

    @Test
    public void testExplodePathsFlatList() {
        final List<String> input = new ArrayList<String>() {{
            add("path/one");
            add("path/two");
        }};
        final List<String> expected = input;

        assertListsEqual(expected, mojo.explodePaths(null, input));
    }

    @Test
    public void testExplodePathsNestedList() {
        final List<String> input = new ArrayList<String>() {{
            add("path/one, path/two   path/three");
            add("path/four");
        }};
        final List<String> expected = new ArrayList<String>() {{
            add("path/one");
            add("path/two");
            add("path/three");
            add("path/four");
        }};

        assertListsEqual(expected, mojo.explodePaths(null, input));
    }

    @Test
    public void testExplodePathsRegexp() {
        final List<String> input = new ArrayList<String>() {{
            add("%regex[.*[GH]oo.*]");
        }};
        final List<String> expected = new ArrayList<String>() {{
            add(path.get("Goo"));
            add(path.get("Hoo"));
        }};

        assertListsEqual(expected, mojo.explodePaths(testDirRoot, input));
    }

        /**
     * Return definition of directory / file layout we'd like to test
     * @return DirNode tree hierarchy
     */
    protected DirNode createLayout() {
        return new DirNode("src",
                new DirNode("test",
                        new DirNode("MyFirstTest.java"),
                        new DirNode("MySecondTest.java")),
                new DirNode("main",
                        new DirNode("Foo.java"),
                        new DirNode("Goo.java"),
                        new DirNode("Hoo.java"),
                        new DirNode("subdir1", new DirNode[0]),
                        new DirNode("subdir1", new DirNode[0]))
        );
    }

    /**
     * Create map (file -> platform dependent path) for assertions
     * @see #createLayout()
     */
    protected Map<String,String> createPathMap() {
        return new HashMap<String, String>() {{
            put("MyFirstTest", FileUtils.getPlatformSpecificPath("src/test/MyFirstTest.java"));
            put("MySecondTest", FileUtils.getPlatformSpecificPath("src/test/MySecondTest.java"));
            put("Foo", FileUtils.getPlatformSpecificPath("src/main/Foo.java"));
            put("Goo", FileUtils.getPlatformSpecificPath("src/main/Goo.java"));
            put("Hoo", FileUtils.getPlatformSpecificPath("src/main/Hoo.java"));
        }};
    }

    /**
     * Return location of Maven's project_dir/target or java.io.tmpdir directory.
     * @return String path
     */
    protected String getTempDirName() {
        String buildDirName = System.getProperty("project.build.directory");
        if (buildDirName == null) {
            buildDirName = System.getProperty("java.io.tmpdir");
        }
        assertNotNull(buildDirName);
        return buildDirName;
    }

    /**
     * Create empty directory in specified location
     * @param parentDir path to parent directory
     * @return File empty directory
     */
    protected File createEmptyDirectory(final String parentDir) {
        final File emptyDir = new File(parentDir, "CloverOptimizerMojoTest");
        FileUtils.deltree(emptyDir);
        emptyDir.mkdirs();
        assertTrue(emptyDir.isDirectory());
        assertEquals(0, emptyDir.list().length);
        return emptyDir;
    }

    /**
     * Create files and directories in specified <code>dir</code> according to layout definition
     * specified in <code>node</code>.
     * @param dir parent directory
     * @param node layout to be created
     * @throws IOException
     */
    protected void createDirectoryLayout(final File dir, final DirNode node) throws IOException {
        final File newNode = new File(dir, node.name);
        if (node.isFile) {
            newNode.createNewFile();
        } else {
            newNode.mkdir();
            for (DirNode child : node.children) {
                createDirectoryLayout(newNode, child);
            }
        }
    }

    protected void assertListsEqual(final List<String> expected, final List<String> actual) {
        final String message = "expected=" + expected.toString() + " actual=" + actual.toString();
        assertTrue(message, expected.containsAll(actual));
        assertTrue(message, actual.containsAll(expected));
    }
}
