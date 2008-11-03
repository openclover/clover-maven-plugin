package com.atlassian.maven.plugin.clover;

import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.cenqua.clover.tasks.CloverSnapshotTask;
import com.cenqua.clover.util.SnapshotDumper;
import com.cenqua.clover.CloverTestSnapshot;
import com.cenqua.clover.CloverNames;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;

import java.io.File;
import java.util.Date;

/**
 * Saves a Clover snapshot to the filesystem.
 *
 * A clover snapshot must be saved after all tests have been run. It is used by subsequent invocations of clover2:optimize
 * to determine which tests get run. Therefore, this file must persist between clean builds.
 *
 * This is possible by using one of the following techniques:
 * 1) set the 'snapshot' (-Dmaven.clover.snapshot) configuration to a location outside the target directory
 * 2) leave the snapshot file in the default location 'target/clover/clover.db.snapshot' and do a clean build with the
 * clover2:clean goal. clover2:clean will delete everything the clean plugin does, however will ensure that the snapshot
 * file does not get deleted.
 *
 * @goal snapshot
 * @phase test
 *
 */
public class CloverSnapshotMojo extends AbstractCloverMojo {

    /**
     *
     * @parameter expression="${maven.clover.span}" 
     */
    private String span;

    /**
     * If set to true, the state of the snapshot file will be dumped to the console.
     *
     * @parameter expression="${maven.clover.snapshot.debug}" default-value="false"
     */
    private boolean debug;

    public void execute() throws MojoExecutionException {

        // only run the snapshot once, on the very last project.
        final MavenProject lastProject = (MavenProject) getReactorProjects().get(getReactorProjects().size() - 1);
        final MavenProject thisProject = getProject();
        if (isSingleCloverDatabase() && !thisProject.equals(lastProject)) {
            getLog().info("Skipping snapshot until the final project in the reactor.");
            return;
        }

        if (skip) {
            getLog().info("Skipping snapshot.");
            return;
        }

         // if there is no database, do not save a snapshot
        if (!new File(resolveCloverDatabase()).exists()) {
            getLog().info(resolveCloverDatabase() + " does not exist. Skipping snapshot creation.");
            return;
        }

        final CloverSnapshotTask task = new CloverSnapshotTask();
        final Project antProj = new Project();
        antProj.init();
        antProj.addBuildListener(new MvnLogBuildListener(getLog()));
        task.setProject(antProj);
        task.init();        
        task.setInitString(resolveCloverDatabase());
        if (span != null) {
            task.setSpan(span);
        } else if (CloverSetupMojo.START_DATE != null) {
            final long timeSinceStart = new Date().getTime() - CloverSetupMojo.START_DATE.getTime();
            final String interval = ((timeSinceStart + 1000)/ 1000) + "s";
            getLog().info("No span specified, using span of: " + interval);
            task.setSpan(interval);
        }

        if (snapshot != null) {
            getLog().info("Saving snapshot to: " + snapshot);
            task.setFile(snapshot);
        }

        task.execute();

        if (getLog().isDebugEnabled() || debug) {
            final String cpLocation = snapshot != null ? snapshot.getPath() : task.getInitString() + CloverNames.SNAPSHOT_SUFFIX;
            SnapshotDumper.printPretty(CloverTestSnapshot.loadFrom(cpLocation), new MvnLogger(getLog()));
        }

    }
}
