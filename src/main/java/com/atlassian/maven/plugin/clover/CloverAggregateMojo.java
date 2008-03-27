package com.atlassian.maven.plugin.clover;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.cenqua.clover.CloverMerge;

/**
 * Aggregate children module Clover databases if there are any. This mojo should not exist. It's only there because
 * the site plugin doesn't handle @aggregators properly at the moment...
 *
 * @goal aggregate
 * @aggregator
 *
 * @version $Id: CloverAggregateMojo.java 555822 2007-07-13 00:03:28Z vsiveton $
 */
public class CloverAggregateMojo extends AbstractCloverMojo
{
    /**
     * The projects in the reactor for aggregation report.
     *
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List reactorProjects;
    
    /**
     * {@inheritDoc}
     * @see com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        if (skip) {
            getLog().debug("Skipping clover aggregate.");
            return;
        }        

        // If we're in a module with children modules, then aggregate the children clover databases.
        if ( getProject().getModules() != null && getProject().getModules().size() > 0 )
        {
            super.execute();

            // Ensure all databases are flushed
            AbstractCloverMojo.waitForFlush( getWaitForFlush(), getFlushInterval() );

            if ( getChildrenCloverDatabases().size() > 0 )
            {
                // Ensure the merged database output directory exists
                new File( getCloverMergeDatabase() ).getParentFile().mkdirs();

                // Merge the databases
                mergeCloverDatabases();
            }
            else
            {
                getLog().warn("No Clover databases found in children projects - No merge done");
            }
        }
    }

    private List getChildrenCloverDatabases()
    {
        // Ideally we'd need to find out where each module stores its Clover
        // database. However that's not
        // currently possible in m2 (see
        // http://jira.codehaus.org/browse/MNG-2180). Thus we'll assume for now
        // that all modules use the cloverDatabase configuration from the top
        // level module.
        
        // Find out the location of the clover DB relative to the root module.
        // Note: This is a pretty buggy algorithm and we really need a proper
        // solution (see MNG-2180)
        
        String relativeCloverDatabasePath = getCloverDatabase().substring(
                getProject().getBasedir().getPath().length());
        
        List dbFiles = new ArrayList();
        
        List projects = getDescendentModuleProjects(getProject());
        
        for (Iterator i = projects.iterator(); i.hasNext();)
        {
            MavenProject childProject = (MavenProject) i.next();
            
            File cloverDb = new File(childProject.getBasedir(),
                    relativeCloverDatabasePath);
            
            if (cloverDb.exists())
            {
                dbFiles.add(cloverDb.getPath());
            }
        }
        
        return dbFiles;
    }
    
    /**
     * returns all the projects that are in the reactor build as
     * direct or indirect modules of the specified project. 
     * 
     * @param project the project to search beneath
     * @return the list of modules that are direct or indirect module descendents
     * of the specified project
     */
    private List getDescendentModuleProjects( MavenProject project )
    {
        return getModuleProjects( project, -1 );
    }
    
    /**
     * Returns all the projects that are modules, or modules of modules, of the 
     * specified project found witin the reactor. 
     * 
     * The searchLevel parameter controls how many descendent levels of modules
     * are returned. With a searchLevels equals to 1, only the immediate modules 
     * of the specified project are returned. 
     * 
     * A searchLevel equals to 2 returns those module's modules as well. 
     * 
     * A searchLevel equals to -1 returns the entire module hierarchy beneath the
     * specified project. Note that this is simply the equivalent to the entire reactor
     * if the specified project is the root execution project.
     * 
     * @param project the project to search under
     * @param levels the number of descendent levels to return
     * @return the list of module projects.
     */
    private List getModuleProjects( final MavenProject project, final int levels )
    {
        List projects = new ArrayList();
        
        boolean infinite = (levels == -1);
        
        if ((reactorProjects != null) && (infinite || levels > 0))
        {            
            for (Iterator i = reactorProjects.iterator(); i.hasNext();)
            {
                MavenProject reactorProject = (MavenProject) i.next();
                
                if (isModuleOfProject(project, reactorProject))
                {
                    projects.add(reactorProject);
                    
                    // recurse to find the modules of this project
                    
                    projects.addAll(getModuleProjects(reactorProject,
                            infinite ? levels : levels - 1));
                }
            }
        }
        
        return projects;
    }  

    /**
     * Returns true if the supplied potentialModule project is a module
     * of the specified parentProject.
     * 
     * @param parentProject
     *            the parent project.
     * @param potentialModule
     *            the potential moduleproject.
     * 
     * @return true if the potentialModule is indeed a module of the specified
     *         parent project.
     */
    private boolean isModuleOfProject( MavenProject parentProject,
            MavenProject potentialModule )
    {
        boolean result = false;
        
        List modules = parentProject.getModules();
        
        if ( modules != null )
        {
            File parentBaseDir = parentProject.getBasedir();

            for (Iterator i = modules.iterator(); i.hasNext();)
            {
                String module = (String) i.next();
                
                File moduleBaseDir = new File( parentBaseDir, module );
                
                try
                {
                    // need these to be canonical paths so we can perform a true equality
                    // operation and remember <module> is a path and for flat multimodule project
                    // structures they will be like this: <module>../a-project<module>
                    
                    String lhs = potentialModule.getBasedir().getCanonicalPath();
                    String rhs = moduleBaseDir.getCanonicalPath();
                    
                    if ( lhs.equals( rhs ))
                    {
                        result = true;
                        break;
                    }
                }
                catch (IOException e)
                {
                    // surpress the exception (?)
                    
                    getLog().error(
                            "error encountered trying to resolve canonical module paths" );
                }                
            }
        }

        return result;
    }
    
    private void mergeCloverDatabases() throws MojoExecutionException
    {
        List dbFiles = getChildrenCloverDatabases();

        List parameters = new ArrayList();

        parameters.add( "-i" );
        parameters.add( getCloverMergeDatabase() );

        if ( getLog().isDebugEnabled() )
        {
           parameters.add( "-d" );
        }

        parameters.addAll( dbFiles );

        int mergeResult = CloverMerge.mainImpl( (String[]) parameters.toArray(new String[0]) );
        if ( mergeResult != 0 )
        {
            throw new MojoExecutionException( "Clover has failed to merge the children module databases" );
        }
    }
}
