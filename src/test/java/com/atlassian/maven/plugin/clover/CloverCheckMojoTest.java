package com.atlassian.maven.plugin.clover;

import org.jmock.integration.junit3.MockObjectTestCase;
import org.jmock.Expectations;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;

import com.atlassian.clover.ant.tasks.CloverPassTask;
import com.atlassian.clover.cfg.Percentage;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;

import java.io.File;
import java.io.IOException;


/**
 */
public class CloverCheckMojoTest extends MockObjectTestCase {


    final MavenProject project = new MavenProject();
    final Project antProject = new Project();
    private File cloverDb;
    private File cloverMergedDb;


    private TestUtil.RecordingLogger log = new TestUtil.RecordingLogger();

    protected void setUp() throws Exception {
        cloverDb = createFile("clover.db");
        cloverMergedDb = createFile("clovermerged.db");
        project.getBuild().setOutputDirectory("target/classes");
        project.getBuild().setDirectory("target");
        project.setFile(new File("pom.xml").getAbsoluteFile());
    }

    public void testWhenDatabaseMissing() throws Exception, FileResourceCreationException, ResourceNotFoundException {
        // ensure this mojo does not failed, if targetPercentage and historyDir are missing
        final CloverPassTask task = new CloverPassTask();
        task.setProject(antProject);
        final CloverCheckMojo mojo = createCheckMojo(task, false);
       
        mojo.execute();
        assertTrue(log.contains("No Clover database found, skipping test coverage verification", TestUtil.Level.INFO));
    }

    // x-ing out due to a license expiry problem
    public void XtestWhenHistoryDirIsMissing() throws Exception, IOException {
        // ensure this mojo does not failed, if targetPercentage and historyDir parameters are missing
        final boolean[] ran = {false};
        final CloverPassTask task = new MockCloverPassTask(ran[0], new Runnable() {
            public void run() {
                ran[0] = true;
            }
        }, antProject);
        CloverCheckMojo mojo = createCheckMojo(task, true);
        final File historyDir = new File("some/nonexistent/path/");
        TestUtil.setPrivateField(CloverCheckMojo.class, mojo, "historyDir", historyDir);

        final TestUtil.RecordingLogger log = new TestUtil.RecordingLogger();
        mojo.setLog(log);
        mojo.execute();
        assertFalse(ran[0]);
        assertTrue(log.contains("Skipping clover:check as 'maven.clover.targetPercentage' is not defined " +
                    "and 'maven.clover.historyDir' (" + historyDir.getPath() +
                    ") does not exist or is not a directory.", TestUtil.Level.WARN));
    }

    // x-ing out due to a license expiry problem    
    public void XtestWithTargetPercentage() throws Exception, IOException {
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
        CloverCheckMojo mojo = createCheckMojo(task, true);

        final Percentage targetPc = new Percentage("95%");
        TestUtil.setPrivateField(CloverCheckMojo.class, mojo, "targetPercentage", targetPc.toString());

        mojo.execute();
        assertTrue(ran[0]);
        assertEquals(targetPc.toString(), pcSet[0].toString());
        assertTrue(log.contains("Checking for coverage of [" + targetPc.toString() +
                                "] for database [" + cloverDb.getPath() + "]",
                                TestUtil.Level.INFO));
    }


    private CloverCheckMojo createCheckMojo(final CloverPassTask task, final boolean areDbsAvailable) throws Exception {
        CloverCheckMojo mojo = new CloverCheckMojo() {
            CloverPassTask createCloverPassTask(String database, final Project antProject) {
                return task;
            }

            public boolean areCloverDatabasesAvailable() {
                return areDbsAvailable;
            }
        };
        mojo.setProject(project);
        mojo.setLog(log);


        final ResourceManager resourceManager = mock(ResourceManager.class);
        checking(new Expectations(){{
            ignoring(resourceManager).addSearchPath("url", "");
            ignoring(resourceManager).addSearchPath(FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath());
            ignoring(resourceManager).getResourceAsFile(with(any(String.class)), with(any(String.class)));
            will(returnValue(new File("/clover.license")));
        }});
        mojo.setResourceManager(resourceManager);
                

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
