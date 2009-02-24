package com.atlassian.maven.plugin.clover.internal.scanner;

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

import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;

import java.util.*;
import java.io.File;

/**
 * Code common to compute the list of source files to instrument (main sources, test sources).
 *
 */
public abstract class AbstractCloverSourceScanner implements CloverSourceScanner
{
    private final CompilerConfiguration configuration;
    private final File targetDir;

    public AbstractCloverSourceScanner(CompilerConfiguration configuration, String outputSourceDirectory)
    {
        this.configuration = configuration;
        this.targetDir = new File(outputSourceDirectory);

    }

    protected CompilerConfiguration getConfiguration()
    {
        return this.configuration;
    }

    /**
     * {@inheritDoc}
     * @see CloverSourceScanner#getSourceFilesToInstrument()
     */
    public Map getSourceFilesToInstrument()
    {
        return computeFiles(getScanner());
    }

    /**
     * {@inheritDoc}
     * @see CloverSourceScanner#getExcludedFiles() 
     */
    public Map getExcludedFiles()
    {
        return computeFiles(getExcludesScanner());
    }

    public Map getResourceFiles() {
        
        return computeFiles(getResourceScanner());
    }

    protected abstract List getSourceRoots();
    protected abstract String getSourceDirectory();

    /**
     * @return a Plexus scanner object that scans a source root and filters files according to inclusion and
     * exclusion patterns. In our case at hand we include only Java sources as these are the only files we want
     * to instrument.
     */
    private SourceInclusionScanner getScanner()
    {
        final SourceInclusionScanner scanner;
        Set includes = getConfiguration().getIncludes();
        Set excludes = getConfiguration().getExcludes();

        if ( includes.isEmpty() && excludes.isEmpty() )
        {
            includes = Collections.singleton( "**/*.java" );
            scanner = new StaleSourceScanner(getConfiguration().getStaleMillis(), includes, Collections.EMPTY_SET );
        }
        else
        {
            if ( includes.isEmpty() )
            {
                includes.add( "**/*.java" );
            }
            scanner = new StaleSourceScanner(getConfiguration().getStaleMillis(), includes, excludes );
        }

        scanner.addSourceMapping( new SuffixMapping( "java", "java" ) );

        return scanner;
    }

    private SourceInclusionScanner getExcludesScanner()
    {

        final SourceInclusionScanner scanner = new StaleSourceScanner(getConfiguration().getStaleMillis(), 
                                                                      getConfiguration().getExcludes(),
                                                                      Collections.EMPTY_SET );

        scanner.addSourceMapping( new SuffixMapping( "java", "java" ) );

        return scanner;
    }

    private SourceInclusionScanner getResourceScanner() {

        Set excludes = Collections.singleton("**/*.java");
        Set includes = Collections.singleton("**");
        final SourceInclusionScanner scanner = new StaleSourceScanner(getConfiguration().getStaleMillis(),
                                                                      includes, excludes);
        
        scanner.addSourceMapping( new SuffixMapping("", "") );

        return scanner;
    }

    private Map computeFiles(SourceInclusionScanner scanner)
    {
        Map files = new HashMap();

        // Decide whether to instrument all source roots or only the main source root.
        Iterator sourceRoots = getResolvedSourceRoots().iterator();
        while ( sourceRoots.hasNext() )
        {
            final File sourceRoot = new File( (String) sourceRoots.next() );
            if ( sourceRoot.exists() )
            {
                try
                {
                    final Set sourcesToAdd = scanner.getIncludedSources( sourceRoot, targetDir);
                    if ( !sourcesToAdd.isEmpty() )
                    {
                        files.put( sourceRoot.getPath(), sourcesToAdd );
                    }
                }
                catch ( InclusionScanException e )
                {
                    getConfiguration().getLog().warn( "Failed to add sources from [" + sourceRoot + "]", e );
                }
            }
        }

        return files;
    }

    private List getResolvedSourceRoots()
    {

        return getConfiguration().includesAllSourceRoots() ?
                getSourceRoots() : 
                Collections.singletonList( getSourceDirectory() );

    }

}
