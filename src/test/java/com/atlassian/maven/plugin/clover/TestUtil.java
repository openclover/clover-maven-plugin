package com.atlassian.maven.plugin.clover;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Field;
import java.util.List;
import java.util.LinkedList;

/**
 */
public class TestUtil {


    private static final SystemStreamLog LOG = new SystemStreamLog();

    private static Log getLog() {
        return LOG;
    }

    public static void setPrivateField(Class clazz, Object target, String fieldName, String value) throws MojoExecutionException {
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            getLog().error("Could not set: " + fieldName, e);
        } catch (IllegalAccessException e) {
            getLog().error("Could not set: " + fieldName, e);
        }
    }

    /**
     * A class to use when unit testing to assert that log statements are correct.
     */
    static class RecordingLogger extends SystemStreamLog {

        final List<LogEvent> buffer = new LinkedList<LogEvent>();

        public enum Level { DEBUG, INFO, WARN, ERROR }

        public boolean contains(String msg, Throwable e, Level level) {
            // look for a log event matching the parameter in the log buffer
            return buffer.contains(new LogEvent(msg, e, level));
        }

        public boolean contains(Throwable e, Level level) {
            // look for a log event matching the parameter in the log buffer
            return buffer.contains(new LogEvent(null, e, level));
        }

        public boolean contains(String msg, Level level) {
            // look for a log event matching the parameter in the log buffer
            return buffer.contains(new LogEvent(msg, null, level));
        }
        

        public void debug(CharSequence content) {
            buffer.add(new LogEvent(content.toString(), null, Level.DEBUG));

        }

        public void debug(CharSequence content, Throwable error) {
            buffer.add(new LogEvent(content.toString(), error, Level.DEBUG));

        }

        public void debug(Throwable error) {
            buffer.add(new LogEvent(null, error, Level.DEBUG));

        }

        public void info(CharSequence content) {
            buffer.add(new LogEvent(content.toString(), null, Level.INFO));
        }

        public void info(CharSequence content, Throwable error) {
            buffer.add(new LogEvent(content.toString(), error, Level.INFO));
        }

        public void info(Throwable error) {
            buffer.add(new LogEvent(null, error, Level.INFO));
        }

        public void warn(CharSequence content) {
            buffer.add(new LogEvent(content.toString(), null, Level.WARN));

        }

        public void warn(CharSequence content, Throwable error) {
            buffer.add(new LogEvent(content.toString(), error, Level.WARN));

        }

        public void warn(Throwable error) {
            buffer.add(new LogEvent(null, error, Level.WARN));

        }

        public void error(CharSequence content) {
            buffer.add(new LogEvent(content.toString(), null, Level.ERROR));

        }

        public void error(CharSequence content, Throwable error) {
            buffer.add(new LogEvent(content.toString(), error, Level.ERROR));

        }

        public void error(Throwable error) {
            buffer.add(new LogEvent(null, error, Level.ERROR));

        }

        class LogEvent {
            Throwable e;
            String msg;
            Level level;

            LogEvent(String msg, Throwable e, Level level) {
                this.e = e;
                this.msg = msg;
                this.level = level;
            }

            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                LogEvent event = (LogEvent) o;

                if (e != null ? !e.equals(event.e) : event.e != null) return false;
                if (level != event.level) return false;
                if (msg != null ? !msg.equals(event.msg) : event.msg != null) return false;

                return true;
            }

            public int hashCode() {
                int result;
                result = (e != null ? e.hashCode() : 0);
                result = 31 * result + (msg != null ? msg.hashCode() : 0);
                result = 31 * result + (level != null ? level.hashCode() : 0);
                return result;
            }
        }
    }
}
