package com.atlassian.maven.plugin.clover.internal;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.PropertyHelper;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;

/**
 * Makes the ${expressions} used in Maven available to Ant as properties.
 */
public class AntPropertyHelper extends PropertyHelper {
    private final Log log;
    private final MavenProject mavenProject;

    /**
     * @param project maven project instance
     * @param l       logger instance
     */
    public AntPropertyHelper(MavenProject project, Log l) {
        mavenProject = project;
        log = l;
    }

    public synchronized Object getPropertyHook(String ns, String name, boolean user) {
        if (log.isDebugEnabled()) {
            log.debug("getProperty(ns=" + ns + ", name=" + name + ", user=" + user + ")");
        }

        return getPropertyHook(ns, name, user, mavenProject);
    }

    private Object getPropertyHook(String ns, String name, boolean user, MavenProject mavenProject) {
        Object val = null;
        try {
            if (name.startsWith("project.")) {
                val = ReflectionValueExtractor.evaluate(
                        name,
                        mavenProject,
                        true
                );
            } else if (name.equals("basedir")) {
                val = ReflectionValueExtractor.evaluate(
                        "basedir.path",
                        mavenProject,
                        false
                );
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Error evaluating expression '" + name + "'", e);
            }
            e.printStackTrace();
        }

        if (val == null) {
            val = super.getPropertyHook(ns, name, user);
            if (val == null) {
                val = System.getProperty(name);
            }
        }

        return val;
    }

}
