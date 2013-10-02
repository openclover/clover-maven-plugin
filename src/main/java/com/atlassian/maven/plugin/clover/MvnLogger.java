package com.atlassian.maven.plugin.clover;

import com.atlassian.clover.Logger;
import org.apache.maven.plugin.logging.Log;

/**
 */
public class MvnLogger extends Logger {

    private final Log log;
    public MvnLogger(Log mvnLog) {
        log = mvnLog;
    }

    public void log(int level, String msg, Throwable t) {
        switch (level) {
            case Logger.LOG_DEBUG: 
            case Logger.LOG_VERBOSE: log.debug(msg, t); break;
            case Logger.LOG_INFO: log.info(msg, t); break;
            case Logger.LOG_WARN: log.warn(msg, t); break;
            default: log.info(msg, t);
        }
    }

    public static class MvnLoggerFactory implements Logger.Factory {

        private final Logger logger;
        public MvnLoggerFactory(Log log) {
            logger = new MvnLogger(log);
        }

        public Logger getLoggerInstance(String category) {
            return logger;
        }
    }

}
