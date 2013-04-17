package com.atlassian.maven.plugin.clover;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.List;

/**
 * Sets the 'test' property on the project which is used by the maven-surefire-plugin to determine which tests are run.
 * If a snapshot file from a previous build, is found, that will be used to determine what tests should be run.
 *
 * @goal optimizeIntegration
 * @phase pre-integration-test
 */
public class CloverIntegrationOptimizerMojo extends CloverOptimizerMojo {

    protected List<String> extractNestedStrings(final String elementName, final Plugin surefirePlugin) {
        List<String> value = null;

        //Try to get the value for the Surefire config for the "integration-test", if there is one
        //but default to the global config otherwise
        final List<PluginExecution> executions = surefirePlugin.getExecutions();
        for (PluginExecution execution : executions) {
            if ("integration-test".equals(execution.getPhase())) {
                Xpp3Dom config = (Xpp3Dom) execution.getConfiguration();
                value = config == null ? null : extractNestedStrings(elementName, config);
                break;
            }
        }

        return value == null ? super.extractNestedStrings(elementName, surefirePlugin) : value;
    }
}
