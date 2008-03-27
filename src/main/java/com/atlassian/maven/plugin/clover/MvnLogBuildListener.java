package com.atlassian.maven.plugin.clover;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.maven.plugin.logging.Log;

/**
 * A simple build listener which logs to maven's Log, instead of Ant's.
 *
 */
public class MvnLogBuildListener extends DefaultLogger {

    private final Log log;

    public MvnLogBuildListener(Log log) {
        this.log = log;
    }

    public void messageLogged(BuildEvent event) {
        switch (event.getPriority()) {
            case Project.MSG_DEBUG:
            case Project.MSG_VERBOSE: log.debug(event.getMessage(), event.getException()); break;
            case Project.MSG_INFO:    log.info(event.getMessage(), event.getException()); break;
            case Project.MSG_WARN:    log.warn(event.getMessage(), event.getException()); break;
            case Project.MSG_ERR:     log.error(event.getMessage(), event.getException()); break;
            default:                  log.debug(event.getMessage(), event.getException());
        }
    }
}
