package com.atlassian.maven.plugin.clover;

import com.atlassian.clover.CloverNames;
import com.atlassian.clover.Logger;
import com.atlassian.clover.ant.tasks.CloverSnapshotTask;
import com.atlassian.clover.optimization.Snapshot;
import com.atlassian.clover.optimization.SnapshotPrinter;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;
import com.atlassian.maven.plugin.clover.internal.ConfigUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;

import java.io.File;
import java.util.Date;

/**
 * Saves a Clover snapshot to the filesystem.
 *
 * A clover snapshot must be saved after all tests have been run. It is used by subsequent invocations of clover:optimize
 * to determine which tests get run. Therefore, this file must persist between clean builds.
 *
 * This is possible by using one of the following techniques:
 * 1) set the 'snapshot' (-Dmaven.clover.snapshot) configuration to a location outside the target directory
 * 2) leave the snapshot file in the default location 'target/clover/clover.db.snapshot' and do a clean build with the
 * clover:clean goal. clover:clean will delete everything the clean plugin does, however will ensure that the snapshot
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
     * If set to true, the snapshot will always be created. Otherwise, if a singleCloverDatabase is used
     * the snapshot will only be created during the execution of the last module in the reactor.
     *
     * @parameter expression="${maven.clover.forceSnapshot}" default-value="false"
     */
    private boolean forceSnapshot;

    public void execute() throws MojoExecutionException {

        // only run the snapshot once, on the very last project.
        if (isSingleCloverDatabase() && !isLastProjectInReactor() && !forceSnapshot) {
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

        final CloverSnapshotTask task = createSnapshotTask();
        task.setInitString(resolveCloverDatabase());
        task.setDebug(debug);

        if (span != null) {
            task.setSpan(span);
        } else if (CloverSetupMojo.START_DATE != null) {
            final long timeSinceStart = new Date().getTime() - CloverSetupMojo.START_DATE.getTime();
            final String interval = ((timeSinceStart + 1000)/ 1000) + "s";
            getLog().info("No span specified, using span of: " + interval);
            task.setSpan(interval);
        }

        snapshot = new ConfigUtil(this).resolveSnapshotFile(snapshot);

        snapshot.getParentFile().mkdirs();
        getLog().info("Saving snapshot to: " + snapshot);
        task.setFile(snapshot);

        execTask(task);

        if (getLog().isDebugEnabled() || debug) {
            final String cpLocation = snapshot != null ? snapshot.getPath() : task.getInitString() + CloverNames.SNAPSHOT_SUFFIX;
            SnapshotPrinter.textPrint(Snapshot.loadFrom(cpLocation), new MvnLogger(getLog()), Logger.LOG_VERBOSE);
        }

    }

    protected void execTask(CloverSnapshotTask task) {
        task.execute();
    }

    CloverSnapshotTask createSnapshotTask() {
        CloverSnapshotTask task = new CloverSnapshotTask();
        final Project antProj = new Project();
        antProj.init();
        antProj.addBuildListener(new MvnLogBuildListener(getLog()));
        task.setProject(antProj);
        task.init();
        return task;
    }
}
