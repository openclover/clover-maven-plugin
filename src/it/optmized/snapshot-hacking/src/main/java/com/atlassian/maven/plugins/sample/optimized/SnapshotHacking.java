package com.atlassian.maven.plugins.sample.optimized;

import org.openclover.runtime.api.CloverException;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageDataSpec;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.optimization.Snapshot;
import org.openclover.core.registry.entities.FullTestCaseInfo;

import java.io.IOException;
import java.util.Set;

/**
 * This class shows how it is possible to modify content of the Clover optimization snapshot.
 * This might be useful in case your project uses non-standard test approach, for instance:
 * <li>manual tests as described on
 * https://openclover.org/doc/manual/latest/hacking--measuring-per-test-coverage-for-manual-tests.html</li>
 * <li>integration tests when tests are executed in integration-test phase but results are checked in verify phase
 * (please note that clover cannot optimize failsafe plugin tests directly)</li>
 * How it works:
 * 1) load CloverDatabase with coverage data and optimization Snapshot
 * 2) walk through all tests recorded and
 * 3) update test result status and/or duration for tests you're interested in
 */
public class SnapshotHacking {
    public static void main(String args[]) throws CloverException, IOException {

        String initstring = "target/clover/clover.db";
        String snapshotLocation = ".clover/clover.snapshot";
        String failedTestCase = "com.atlassian.maven.plugin.clover.samples.modulea.SimpleTest.testSomeMethod";

        System.out.println("Loading " + initstring + " and " + snapshotLocation);
        CloverDatabase db = CloverDatabase.loadWithCoverage(initstring, new CoverageDataSpec());
        Snapshot snapshot = Snapshot.loadFrom(snapshotLocation);

        System.out.println("Looking for " + failedTestCase);
        final Set<TestCaseInfo> allTestCaseInfos = db.getCoverageData().getTests();
        for (TestCaseInfo tci : allTestCaseInfos) {
            // simple form 'tci.getTestName().equals("testSomeMethod")' is also possible
            if (tci.getQualifiedName().equals(failedTestCase)) {
                // our test lasted 1 milisecond (it's used for ordering tests)
                long duration = 1;
                // our test has failed; TestCaseInfo is read-only, mutation needs the concrete type
                ((FullTestCaseInfo) tci).setFailure(true);
                // update snapshot
                snapshot.updatePerTestInfo(db, tci, duration);
            }
        }

        System.out.println("Saving snapshot file");
        snapshot.store();
    }
}
