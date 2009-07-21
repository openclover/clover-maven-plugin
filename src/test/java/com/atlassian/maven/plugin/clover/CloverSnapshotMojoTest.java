package com.atlassian.maven.plugin.clover;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.cenqua.clover.tasks.CloverSnapshotTask;
import org.jmock.lib.legacy.ClassImposteriser;

/**
 */
public class CloverSnapshotMojoTest extends MockObjectTestCase {

    CloverSnapshotMojo mojo;
    final MavenProject project = new MavenProject();
    private TestUtil.RecordingLogger log = new TestUtil.RecordingLogger();


    protected void setUp() throws Exception {
        super.setUp();
        setImposteriser(ClassImposteriser.INSTANCE);
        project.getBuild().setDirectory("target");

    }

    public void testExecuteCloverSnapshotWhenSnapshotDirDoesNotExist() throws MojoExecutionException, IOException {

        final CloverSnapshotTask task = mock(CloverSnapshotTask.class);
        mojo = new CloverSnapshotMojo() {
            public boolean isSingleCloverDatabase() {
                return true;
            }

            public List getReactorProjects() {
                final ArrayList list = new ArrayList();
                list.add(project);
                return list;
            }

            CloverSnapshotTask createSnapshotTask() {
                return task;
            }

            protected void execTask(CloverSnapshotTask task) {
                
            }
        };
        mojo.setLog(log);
        mojo.setProject(project);
        final File snapshot = new File("test-clover/clover.snapshot");

        checking(new Expectations(){{
            oneOf(task).setFile(snapshot);
            oneOf(task).setInitString("target/clover/clover.db");
            oneOf(task).setDebug(false);
        }});


        TestUtil.setPrivateParentField(CloverSnapshotMojo.class, mojo, "snapshot", snapshot);
        final File db = new File(mojo.resolveCloverDatabase());
        db.getParentFile().mkdirs();
        db.createNewFile();
        mojo.execute();
        assertTrue(log.contains("Saving snapshot to: test-clover" + File.separator + "clover.snapshot", TestUtil.Level.INFO));
        assertTrue(snapshot.getParentFile().exists());
    }
}
