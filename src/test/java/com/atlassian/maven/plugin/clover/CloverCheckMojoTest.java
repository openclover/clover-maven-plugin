package com.atlassian.maven.plugin.clover;

import org.jmock.MockObjectTestCase;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;

/**
 */
public class CloverCheckMojoTest extends MockObjectTestCase {


    final MavenProject project = new MavenProject();

    protected void setUp() throws Exception {
        project.getBuild().setOutputDirectory("target/classes");
        project.getBuild().setDirectory("target");
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
        assertTrue(log.contains("No Clover database found, skipping test coverage verification", TestUtil.RecordingLogger.Level.INFO));
    }



}
