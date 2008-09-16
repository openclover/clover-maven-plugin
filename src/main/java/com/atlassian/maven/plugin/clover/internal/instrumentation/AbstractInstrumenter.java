package com.atlassian.maven.plugin.clover.internal.instrumentation;

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

import com.atlassian.maven.plugin.clover.MvnLogger;
import com.atlassian.maven.plugin.clover.internal.CloverConfiguration;
import com.atlassian.maven.plugin.clover.internal.scanner.CloverSourceScanner;
import com.cenqua.clover.CloverInstr;
import com.cenqua.clover.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Code common for instrumentation of various source roots (main sources, test sources).
 *
 * @version $Id: $
 */
public abstract class AbstractInstrumenter
{
    private CloverConfiguration configuration;

    private String outputSourceDirectory;

    public AbstractInstrumenter(CloverConfiguration configuration, String outputSourceDirectory)
    {
        this.configuration = configuration;
        this.outputSourceDirectory = outputSourceDirectory;
    }

    protected CloverConfiguration getConfiguration()
    {
        return this.configuration;
    }

    public void instrument() throws MojoExecutionException
    {
        CloverSourceScanner scanner = getSourceScanner();
        Map filesToInstrument = scanner.getSourceFilesToInstrument();
        if ( filesToInstrument.isEmpty() )
        {
            getConfiguration().getLog().warn( "No Clover instrumentation done on source files as no "
                + "matching sources files found" );
        }
        else
        {
            instrumentSources( filesToInstrument, outputSourceDirectory );

        }

        // We need to copy excluded files as otherwise they won't be in the new Clover source directory and
        // thus won't be compiled by the compile plugin. This will lead to compilation errors if any other
        // Java file depends on any of these excluded files.
        copyExcludedFiles( scanner.getExcludedFiles(), outputSourceDirectory );

        //won't do its job when include files set is empty!
    }

    public void redirectSourceDirectories()
    {
        redirectSourceDirectories( outputSourceDirectory );
    }

    protected abstract CloverSourceScanner getSourceScanner();
    protected abstract String getSourceDirectory();
    protected abstract void setSourceDirectory(String targetDirectory);
    protected abstract List getCompileSourceRoots(); 
    protected abstract void addCompileSourceRoot(String sourceRoot);
    
    private void redirectSourceDirectories(String targetDirectory)
    {
        String oldSourceDirectory = getSourceDirectory();

        if ( new File( oldSourceDirectory ).exists() )
        {
            setSourceDirectory( targetDirectory );
        }

        getConfiguration().getLog().debug( "Clover source directories before change:" );
        logSourceDirectories();

        // Maven2 limitation: changing the source directory doesn't change the compile source roots
        // See http://jira.codehaus.org/browse/MNG-1945
        List sourceRoots = new ArrayList( getCompileSourceRoots() );

        // Clean all source roots to add them again in order to keep the same original order of source roots.
        getCompileSourceRoots().removeAll( sourceRoots );

        for ( Iterator i = sourceRoots.iterator(); i.hasNext(); )
        {
            String sourceRoot = (String) i.next();
            if ( new File( oldSourceDirectory ).exists() && sourceRoot.equals( oldSourceDirectory ) )
            {
                addCompileSourceRoot( getSourceDirectory() );
            }
            // ignore the generated sources directory, because a new clover/generated-sources directory has been created by a plugin
            // during the clover forked lifecycle, which means that we will end up with the same classes included twice
            // if we use the generated-classes directory added originally
            else if (!isGeneratedSourcesDirectory(sourceRoot) && !getConfiguration().includesAllSourceRoots())
            {
                addCompileSourceRoot( sourceRoot );
            }
        }

        getConfiguration().getLog().debug( "Clover main source directories after change:" );
        logSourceDirectories();
    }

    private boolean isGeneratedSourcesDirectory(String sourceRoot) {
        String generatedSourcesDirectoryName = File.separator + "target" + File.separator + "generated-sources";
        return sourceRoot.indexOf(generatedSourcesDirectoryName) != -1;
    }

    private void logSourceDirectories()
    {
        if ( getConfiguration().getLog().isDebugEnabled() )
        {
            for ( Iterator i = getCompileSourceRoots().iterator(); i.hasNext(); )
            {
                String sourceRoot = (String) i.next();
                getConfiguration().getLog().debug( "[Clover]  source root [" + sourceRoot + "]" );
            }
        }
    }

    /**
     * Copy all files that have been excluded by the user (using the excludes configuration property). This is required
     * as otherwise the excluded files won't be in the new Clover source directory and thus won't be compiled by the
     * compile plugin. This will lead to compilation errors if any other Java file depends on any of them.
     *
     * @throws MojoExecutionException if a failure happens during the copy
     */
    private void copyExcludedFiles(Map excludedFiles, String targetDirectory) throws MojoExecutionException
    {
        for ( Iterator sourceRoots = excludedFiles.keySet().iterator(); sourceRoots.hasNext(); )
        {
            String sourceRoot = (String) sourceRoots.next();
            Set filesInSourceRoot = (Set) excludedFiles.get( sourceRoot );

            for ( Iterator files = filesInSourceRoot.iterator(); files.hasNext(); )
            {
                File file = (File) files.next();

                try
                {
                    FileUtils.copyFile( file, new File( targetDirectory,
                        file.getPath().substring(sourceRoot.length() ) ) );
                }
                catch (IOException e)
                {
                    throw new MojoExecutionException( "Failed to copy excluded file [" + file + "] to ["
                        + targetDirectory + "]", e );
                }
            }
        }
    }

    private void instrumentSources( Map filesToInstrument, String outputDir ) throws MojoExecutionException {

        Logger.setInstance(new MvnLogger(configuration.getLog()));
        int result = CloverInstr.mainImpl( createCliArgs( filesToInstrument, outputDir ) );
        if ( result != 0 )
        {
            throw new MojoExecutionException( "Clover has failed to instrument the source files "
                + "in the [" + outputDir + "] directory" );
        }
    }

    /**
     * @return the CLI args to be passed to CloverInstr
     */
    private String[] createCliArgs( Map filesToInstrument,  String outputDir ) throws MojoExecutionException
    {
        List parameters = new ArrayList();

        parameters.add( "-p" );
        parameters.add( getConfiguration().getFlushPolicy() );
        parameters.add( "-f" );
        parameters.add( "" + getConfiguration().getFlushInterval() );

        parameters.add( "-i" );
        parameters.add( getConfiguration().getCloverDatabase() );

        parameters.add( "-d" );
        parameters.add( outputDir );

        if ( getConfiguration().getLog().isDebugEnabled() )
        {
            parameters.add( "-v" );
        }

        if ( getConfiguration().getJdk() != null )
        {
           if ( getConfiguration().getJdk().equals( "1.4" ) )
            {
                parameters.add( "--source" );
                parameters.add( "1.4" );
            }
            else if ( getConfiguration().getJdk().equals( "1.5" ) )
            {
                parameters.add( "--source" );
                parameters.add( "1.5" );
            }
            else if ( getConfiguration().getJdk().equals( "1.6" ) )
            {
                parameters.add( "--source" );
                parameters.add( "1.6" );
            }
            else
            {
                throw new MojoExecutionException( "Unsupported jdk version [" + getConfiguration().getJdk()
                    + "]. Valid values are [1.4], [1.5] and [1.6]" );
            }
        }

        if (!getConfiguration().isUseFullyQualifiedJavaLang())
        {
            parameters.add("--dontFullyQualifyJavaLang");
        }

        if (getConfiguration().getEncoding() != null) {
            parameters.add("--encoding");
            parameters.add(getConfiguration().getEncoding());
        }

        for ( Iterator sourceRoots = filesToInstrument.keySet().iterator(); sourceRoots.hasNext(); )
        {
            Set filesInSourceRoot = (Set) filesToInstrument.get( (String) sourceRoots.next() );
            for ( Iterator files = filesInSourceRoot.iterator(); files.hasNext(); )
            {
                File file = (File) files.next();
                parameters.add( file.getPath() );
            }
        }

        // custom contexts
        addCustomContexts(parameters, getConfiguration().getMethodContexts().entrySet(), "-mc");
        addCustomContexts(parameters, getConfiguration().getStatementContexts().entrySet(), "-sc");

        // Log parameters
        if ( getConfiguration().getLog().isDebugEnabled() )
        {
            getConfiguration().getLog().debug( "Parameter list being passed to Clover CLI:" );
            for ( Iterator it = parameters.iterator(); it.hasNext(); )
            {
                String param = (String) it.next();
                getConfiguration().getLog().debug( "  parameter = [" + param + "]" );
            }
        }

        return (String[]) parameters.toArray( new String[0] );
    }

    private void addCustomContexts(List parameters, Set/*<Map.Entry<<String,<String>>>*/ contexts, String flag) {
        for (Iterator iterator = contexts.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            parameters.add(flag);
            parameters.add(entry.getKey() + "=" + entry.getValue());
        }
    }
}

