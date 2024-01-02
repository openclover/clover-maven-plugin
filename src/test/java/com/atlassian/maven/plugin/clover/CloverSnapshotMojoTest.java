package com.atlassian.maven.plugin.clover;

import com.atlassian.clover.ant.tasks.CloverSnapshotTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class CloverSnapshotMojoTest {

    private final MavenProject project = new MavenProject();
    private final TestUtil.RecordingLogger log = new TestUtil.RecordingLogger();

    private class CloverSnapshotMojoStub extends CloverSnapshotMojo {
        private final CloverSnapshotTask snapshotTask;

        CloverSnapshotMojoStub(CloverSnapshotTask snapshotTask) {
            this.snapshotTask = snapshotTask;
        }

        @Override
        public boolean isSingleCloverDatabase() {
            return true;
        }

        @Override
        public List<MavenProject> getReactorProjects() {
            final ArrayList<MavenProject> list = new ArrayList<>();
            list.add(project);
            return list;
        }

        @Override
        CloverSnapshotTask createSnapshotTask() {
            return snapshotTask;
        }

        @Override
        protected void execTask(CloverSnapshotTask task) {

        }
    }

    @Before
    public void setUp() {
        project.getBuild().setDirectory("target");
    }

    @Test
    public void testExecuteCloverSnapshotWhenSnapshotDirDoesNotExist() throws MojoExecutionException, IOException {
        final CloverSnapshotTask task = mock(CloverSnapshotTask.class);
        CloverSnapshotMojo mojo = new CloverSnapshotMojoStub(task);
        mojo.setLog(log);
        mojo.setProject(project);
        final File snapshot = new File("target/clover.snapshot");

        TestUtil.setPrivateParentField(CloverSnapshotMojo.class, mojo, "snapshot", snapshot);
        final File db = new File(mojo.resolveCloverDatabase());
        db.getParentFile().mkdirs();
        assertTrue(db.createNewFile());

        mojo.execute();

        verify(task, times(1)).setFile(snapshot);
        verify(task, times(1)).setInitString("target/clover/clover.db");
        verify(task, times(1)).setDebug(false);

        assertTrue(log.contains("Saving snapshot to: target" + File.separator + "clover.snapshot", TestUtil.Level.INFO));
        assertTrue(snapshot.getParentFile().exists());
    }
}
