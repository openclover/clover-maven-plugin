package org.openclover.maven.plugin.internal;

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
 * <p>
 * Registered with Ant's PropertyHelper via PropertyHelper.add(PropertyHelper.PropertyEvaluator);
 * returning {@code null} lets Ant's own property resolution (including system properties) take over.
 */
public class AntPropertyHelper implements PropertyHelper.PropertyEvaluator {
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

    @Override
    public Object evaluate(String property, PropertyHelper propertyHelper) {
        if (log.isDebugEnabled()) {
            log.debug("evaluate(property=" + property + ")");
        }

        try {
            if (property.startsWith("project.")) {
                return ReflectionValueExtractor.evaluate(
                        property,
                        mavenProject,
                        true
                );
            } else if (property.equals("basedir")) {
                return ReflectionValueExtractor.evaluate(
                        "basedir.path",
                        mavenProject,
                        false
                );
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Error evaluating expression '" + property + "'", e);
            }
            e.printStackTrace();
        }

        // let Ant's own property resolution (chained evaluators, system properties, ...) handle it
        return null;
    }

}
