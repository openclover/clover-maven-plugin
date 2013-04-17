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

    public static void setPrivateField(final Class clazz, final Object target, final String fieldName, final Object value) throws MojoExecutionException {
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


    public static void setPrivateParentField(final Class clazz, final Object target, final String fieldName, final Object value) throws MojoExecutionException {
        try {
            final Field field = clazz.getSuperclass().getDeclaredField(fieldName);
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

        public boolean contains(String msg, Throwable e, int level) {
            // look for a log event matching the parameter in the log buffer
            return buffer.contains(new LogEvent(msg, e, level));
        }

        public boolean contains(Throwable e, int level) {
            // look for a log event matching the parameter in the log buffer
            return buffer.contains(new LogEvent(null, e, level));
        }

        public boolean contains(String msg, int level) {
            // look for a log event matching the parameter in the log buffer
            return buffer.contains(new LogEvent(msg, null, level));
        }
        

        public void debug(CharSequence content) {
            super.debug(content);
            buffer.add(new LogEvent(content.toString(), null, Level.DEBUG));

        }

        public void debug(CharSequence content, Throwable error) {
            super.debug(content);
            buffer.add(new LogEvent(content.toString(), error, Level.DEBUG));
        }

        public void debug(Throwable error) {
            super.debug(error);
            buffer.add(new LogEvent(null, error, Level.DEBUG));

        }

        public void info(CharSequence content) {
            super.info(content);
            buffer.add(new LogEvent(content.toString(), null, Level.INFO));
        }

        public void info(CharSequence content, Throwable error) {
            if (error != null) {
                super.info(content, error);
            } else {
                super.info(content);
            }
            buffer.add(new LogEvent(content.toString(), error, Level.INFO));
        }

        public void info(Throwable error) {
            super.info(error);
            buffer.add(new LogEvent(null, error, Level.INFO));
        }

        public void warn(CharSequence content) {
            super.warn(content);
            buffer.add(new LogEvent(content.toString(), null, Level.WARN));

        }

        public void warn(CharSequence content, Throwable error) {
            if (error != null) {
                super.warn(content, error);
            } else {
                super.warn(content);
            }
            buffer.add(new LogEvent(content.toString(), error, Level.WARN));
        }

        public void warn(Throwable error) {
            super.warn(error);
            buffer.add(new LogEvent(null, error, Level.WARN));

        }

        public void error(CharSequence content) {
            super.error(content);
            buffer.add(new LogEvent(content.toString(), null, Level.ERROR));

        }

        public void error(CharSequence content, Throwable error) {
            if (error != null) {
                super.error(content, error);
            } else {
                super.error(content);
            }
            buffer.add(new LogEvent(content.toString(), error, Level.ERROR));

        }

        public void error(Throwable error) {
            super.error(error);
            buffer.add(new LogEvent(null, error, Level.ERROR));

        }

        class LogEvent {
            Throwable e;
            String msg;
            int level;

            LogEvent(String msg, Throwable e, int level) {
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
                result = 31 * result + level;
                return result;
            }
        }
    }

    public class Level {
        public static final int DEBUG = 0;
        public static final int INFO = 1;
        public static final int WARN = 2;
        public static final int ERROR = 3;
    }
}
