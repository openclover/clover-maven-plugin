package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.atlassian.maven.plugin.clover.internal.ConfigUtil;
import com.cenqua.clover.CloverNames;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.taskdefs.Delete;

import java.io.File;

/**
 * The clover2:clean goal should be run directly from the command line,
 * when you are developing using the clover test runner optimizer.
 *
 * This mojo ensures that the file required by Clover to optimize your test is not deleted between builds.
 * 
 * @goal clean
 * @phase initialize
 */
public class CloverCleanMojo extends AbstractCloverMojo {

    public void execute() throws MojoExecutionException {

        final Project project = new Project();
        project.setBasedir(getProject().getBasedir().getPath());
        project.addBuildListener(new MvnLogBuildListener(getLog()));
        project.init();
        // delete just the snapshot
        final File snapshotFile = new ConfigUtil(this).resolveSnapshotFile(snapshot);
        removeFile(snapshotFile, project);
    }

    private void removeFile(File snapshot, Project project) throws MojoExecutionException {

        if (!snapshot.exists() || snapshot.isDirectory()) {
            return;
        }
        Delete delete = new Delete();
        delete.setProject(project);
        delete.setIncludeEmptyDirs(true);
        delete.init();
        delete.setFile(snapshot);
        delete.execute();
        if (snapshot.exists()) {
            getLog().warn("clover2:clean could not delete file: " + snapshot);
        }
    }
}
