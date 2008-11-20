package com.atlassian.maven.plugin.clover;

import org.jmock.integration.junit3.MockObjectTestCase;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.FileUtils;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.cenqua.clover.tasks.CloverPassTask;
import com.cenqua.clover.cfg.Percentage;

import java.io.File;
import java.io.IOException;


/**
 */
public class CloverCheckMojoTest extends MockObjectTestCase {


    final MavenProject project = new MavenProject();
    final Project antProject = new Project();
    private String license;
    private File cloverDb;
    private File cloverMergedDb;


    private TestUtil.RecordingLogger log = new TestUtil.RecordingLogger();

    protected void setUp() throws Exception {
        cloverDb = createFile("clover.db");
        cloverMergedDb = createFile("clovermerged.db");
        project.getBuild().setOutputDirectory("target/classes");
        project.getBuild().setDirectory("target");
        license = FileUtils.fileRead(new File(getClass().getResource("/clover.license").toURI()));
    }

    public void testWhenDatabaseMissing() throws MojoExecutionException {
        // ensure this mojo does not failed, if targetPercentage and historyDir are missing
        CloverCheckMojo mojo = new CloverCheckMojo();    
        mojo.setProject(project);
        final TestUtil.RecordingLogger log = new TestUtil.RecordingLogger();
        mojo.setLog(log);
        TestUtil.setPrivateField(AbstractCloverMojo.class, mojo, "cloverDatabase", "target/clover/clover.db");
        TestUtil.setPrivateField(AbstractCloverMojo.class, mojo, "cloverMergeDatabase", "target/clover/clovermerged.db");
        mojo.execute();
        assertTrue(log.contains("No Clover database found, skipping test coverage verification", TestUtil.Level.INFO));
    }
    
    public void testWhenHistoryDirIsMissing() throws MojoExecutionException, IOException {
        // ensure this mojo does not failed, if targetPercentage and historyDir parameters are missing
        final boolean[] ran = {false};
        final CloverPassTask task = new MockCloverPassTask(ran[0], new Runnable() {
            public void run() {
                ran[0] = true;
            }
        }, antProject);
        CloverCheckMojo mojo = createCheckMojo(task);
        final File historyDir = new File("some/nonexistent/path/");
        TestUtil.setPrivateField(CloverCheckMojo.class, mojo, "historyDir", historyDir);

        final TestUtil.RecordingLogger log = new TestUtil.RecordingLogger();
        mojo.setLog(log);
        mojo.execute();
        assertFalse(ran[0]);
        assertTrue(log.contains("Skipping clover2:check as 'maven.clover.targetPercentage' is not defined " +
                    "and 'maven.clover.historyDir' (" + historyDir.getPath() +
                    ") does not exist or is not a directory.", TestUtil.Level.WARN));
    }

    public void testWithTargetPercentage() throws MojoExecutionException, IOException {
        // ensure this mojo does not failed, if targetPercentage and historyDir parameters are missing

        final boolean[] ran = {false};
        final Runnable closure = new Runnable() {
            public void run() {
                ran[0] = true;
            }
        };

        final Percentage[] pcSet = {null};
        final CloverPassTask task = new MockCloverPassTask(ran[0], closure, antProject) {
            public void setTarget(Percentage percentValue) {
                super.setTarget(percentValue);
                pcSet[0] = percentValue;
            }
        };
        CloverCheckMojo mojo = createCheckMojo(task);

        final Percentage targetPc = new Percentage("95%");
        TestUtil.setPrivateField(CloverCheckMojo.class, mojo, "targetPercentage", targetPc.toString());

        mojo.execute();
        assertTrue(ran[0]);
        assertEquals(targetPc.toString(), pcSet[0].toString());
        assertTrue(log.contains("Checking for coverage of [" + targetPc.toString() +
                                "] for database [" + cloverDb.getPath() + "]",
                                TestUtil.Level.INFO));
    }


    private CloverCheckMojo createCheckMojo(final CloverPassTask task) throws MojoExecutionException, IOException {
        CloverCheckMojo mojo = new CloverCheckMojo() {
            CloverPassTask createCloverPassTask(String database, final Project antProject) {
                return task;
            }
        };
        mojo.setProject(project);
        mojo.setLicense(license);
        mojo.setLog(log);        

        TestUtil.setPrivateField(AbstractCloverMojo.class, mojo, "cloverDatabase", cloverDb.getPath());
        TestUtil.setPrivateField(AbstractCloverMojo.class, mojo, "cloverMergeDatabase", cloverMergedDb.getPath());
        return mojo;
    }

    private File createFile(String s) throws IOException {
        File cloverDb = File.createTempFile(s, "");
        cloverDb.createNewFile();
        return cloverDb;
    }


    private static class MockCloverPassTask extends CloverPassTask {
        private final boolean passed;
        private final Runnable closure;
        private final Project antProject;

        public MockCloverPassTask(boolean passed, Runnable closure, Project antProject) {
            this.passed = passed;
            this.closure = closure;
            this.antProject = antProject;
        }

        public void cloverExecute() {
            getProject().setProperty("passed", Boolean.toString(passed));
            closure.run();
        }

        public Project getProject() {
            return antProject;
        }

    }
}
