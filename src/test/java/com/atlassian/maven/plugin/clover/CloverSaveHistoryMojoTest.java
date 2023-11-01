package com.atlassian.maven.plugin.clover;

import com.atlassian.clover.ant.tasks.HistoryPointTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link CloverSaveHistoryMojo}
 */
public class CloverSaveHistoryMojoTest {

    private static class CloverSaveHistoryMojoStub extends CloverSaveHistoryMojo {
        private final MavenProject project;
        private final HistoryPointTask task;

        CloverSaveHistoryMojoStub(MavenProject project, HistoryPointTask task) {
            this.project = project;
            this.task = task;
        }

        @Override
        public List<MavenProject> getReactorProjects() {
            final ArrayList<MavenProject> list = new ArrayList<>();
            list.add(project);
            return list;
        }

        @Override
        HistoryPointTask createHistoryTask(Project antProject) {
            return task;
        }

        @Override
        protected boolean areCloverDatabasesAvailable() {
            return true;
        }

        @Override
        protected void executeTask(HistoryPointTask cloverHistoryTask) {
        }

        @Override
        protected String getCloverMergeDatabase() {
            return "nonexisting";
        }
    }

    private CloverSaveHistoryMojo mojo;
    private HistoryPointTask task;

    private final MavenProject project = new MavenProject();
    private final TestUtil.RecordingLogger log = new TestUtil.RecordingLogger();
    private File db;

    @Before
    public void setUp() throws Exception {
        project.getBuild().setDirectory("target");
        task = mock(HistoryPointTask.class);

        mojo = new CloverSaveHistoryMojoStub(project, task);

        mojo.setLog(log);
        mojo.setProject(project);

        db = new File(mojo.resolveCloverDatabase());
        db.getParentFile().mkdirs();
        db.createNewFile();
    }

    @After
    public void tearDown() {
        db.delete();
    }

    @Test
    public void testExecuteCloverSaveHistoryWithPathRelative() throws MojoExecutionException {
        final String historyDir = ".cloverhistory";

        TestUtil.setPrivateField(CloverSaveHistoryMojo.class, mojo, "historyDir", historyDir);

        mojo.execute();

        verify(task, times(1)).init();
        verify(task, times(1)).setInitString(mojo.resolveCloverDatabase());
        verify(task, times(1)).setHistoryDir(new File(project.getBasedir(), historyDir));

        assertTrue(log.contains("Saving Clover history point for database [" + mojo.resolveCloverDatabase() + "] in [" + historyDir + "]", TestUtil.Level.INFO));
    }

    @Test
    public void testExecuteCloverSaveHistoryWithPathAbsolute() throws MojoExecutionException {
        final String historyDir = new File(project.getBasedir(), ".cloverhistory").getAbsolutePath();

        TestUtil.setPrivateField(CloverSaveHistoryMojo.class, mojo, "historyDir", historyDir);

        mojo.execute();

        verify(task, times(1)).init();
        verify(task, times(1)).setInitString(mojo.resolveCloverDatabase());
        verify(task, times(1)).setHistoryDir(new File(historyDir));

        assertTrue(log.contains("Saving Clover history point for database [" + mojo.resolveCloverDatabase() + "] in [" + historyDir + "]", TestUtil.Level.INFO));
    }
}
