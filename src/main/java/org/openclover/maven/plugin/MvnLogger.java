package org.openclover.maven.plugin;

import org.apache.maven.plugin.logging.Log;
import org.openclover.runtime.Logger;

public class MvnLogger extends Logger {

    private final Log log;

    public MvnLogger(Log mvnLog) {
        log = mvnLog;
    }

    public void log(int level, String msg, Throwable t) {
        switch (level) {
            case Logger.LOG_ERR:
                log.error(msg, t);
                break;
            case Logger.LOG_WARN:
                log.warn(msg, t);
                break;
            case Logger.LOG_INFO:
                log.info(msg, t);
                break;
            default:
                log.debug(msg, t);
                break;
        }
    }

}