package com.atlassian.maven.plugin.clover;

import com.atlassian.clover.ant.tasks.HistoryPointTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.jmock.lib.legacy.ClassImposteriser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link CloverSaveHistoryMojo}
 */
public class CloverSaveHistoryMojoTest extends MockObjectTestCase {

    private CloverSaveHistoryMojo mojo;
    private HistoryPointTask task;

    final MavenProject project = new MavenProject();
    private TestUtil.RecordingLogger log = new TestUtil.RecordingLogger();
    private File db;

    protected void setUp() throws Exception {
        super.setUp();
        setImposteriser(ClassImposteriser.INSTANCE);

        project.getBuild().setDirectory("target");
        task = mock(HistoryPointTask.class);

        mojo = new CloverSaveHistoryMojo() {

            public List<MavenProject> getReactorProjects() {
                final ArrayList<MavenProject> list = new ArrayList<MavenProject>();
                list.add(project);
                return list;
            }

            HistoryPointTask createHistoryTask(Project antProject) {
                return task;
            }

            @Override
            protected boolean areCloverDatabasesAvailable() {
                return true;
            }

            @Override
            protected void registerLicenseFile() throws MojoExecutionException {
            }

            @Override
            protected void executeTask(HistoryPointTask cloverHistoryTask) {
            }

            @Override
            protected String getCloverMergeDatabase() {
                return "nonexisting";
            }
        };

        mojo.setLog(log);
        mojo.setProject(project);

        db = new File(mojo.resolveCloverDatabase());
        db.getParentFile().mkdirs();
        db.createNewFile();
    }

    @Override
    protected void tearDown() throws Exception {
        db.delete();
        super.tearDown();
    }

    public void testExecuteCloverSaveHistoryWithPathRelative() throws MojoExecutionException, IOException {

        final String historyDir = ".cloverhistory";

        TestUtil.setPrivateField(CloverSaveHistoryMojo.class, mojo, "historyDir", historyDir);

        checking(new Expectations() {{
            oneOf(task).init();
            oneOf(task).setInitString(mojo.resolveCloverDatabase());
            oneOf(task).setHistoryDir(new File(project.getBasedir(), historyDir));
        }});

        mojo.execute();

        assertTrue(log.contains("Saving Clover history point for database [" + mojo.resolveCloverDatabase() + "] in [" + historyDir + "]", TestUtil.Level.INFO));
    }

    public void testExecuteCloverSaveHistoryWithPathAbsolute() throws MojoExecutionException, IOException {

        final String historyDir = new File (project.getBasedir(), ".cloverhistory").getAbsolutePath();

        TestUtil.setPrivateField(CloverSaveHistoryMojo.class, mojo, "historyDir", historyDir);

        checking(new Expectations() {{
            oneOf(task).init();
            oneOf(task).setInitString(mojo.resolveCloverDatabase());
            oneOf(task).setHistoryDir(new File(historyDir));
        }});

        mojo.execute();

        assertTrue(log.contains("Saving Clover history point for database [" + mojo.resolveCloverDatabase() + "] in [" + historyDir + "]", TestUtil.Level.INFO));
    }
}
