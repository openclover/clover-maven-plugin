package com.atlassian.maven.plugin.clover;

import com.atlassian.clover.ant.tasks.CloverSnapshotTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.jmock.lib.legacy.ClassImposteriser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

            public List<MavenProject> getReactorProjects() {
                final ArrayList<MavenProject> list = new ArrayList<MavenProject>();
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
        final File snapshot = new File("target/clover.snapshot");

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
        assertTrue(log.contains("Saving snapshot to: target" + File.separator + "clover.snapshot", TestUtil.Level.INFO));
        assertTrue(snapshot.getParentFile().exists());
    }
}
