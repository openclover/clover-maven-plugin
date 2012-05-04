package com.atlassian.maven.plugin.clover;

/**
 * Sets the 'test' property on the project which is used by the maven-surefire-plugin to determine which tests are run.
 * If a snapshot file from a previous build, is found, that will be used to determine what tests should be run.
 *
 * @goal optimizeIntegration
 * @phase pre-integration-test
 */
public class CloverIntegrationOptimizerMojo extends CloverOptimizerMojo {
}
