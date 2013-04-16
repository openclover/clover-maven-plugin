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
import com.atlassian.maven.plugin.clover.internal.CompilerConfiguration;
import com.atlassian.maven.plugin.clover.internal.scanner.CloverSourceScanner;
import com.atlassian.maven.plugin.clover.internal.scanner.GroovyMainSourceScanner;
import com.atlassian.maven.plugin.clover.internal.scanner.GroovyTestSourceScanner;
import com.atlassian.maven.plugin.clover.internal.scanner.LanguageFileExtensionFilter;
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
 */
public abstract class AbstractInstrumenter
{
    private CompilerConfiguration configuration;

    String outputSourceDirectory;
    private static final String PROP_PROJECT_BUILD_SOURCEENCODING = "project.build.sourceEncoding";

    public AbstractInstrumenter(CompilerConfiguration configuration, String outputSourceDirectory)
    {
        this.configuration = configuration;
        this.outputSourceDirectory = outputSourceDirectory;
    }

    protected CompilerConfiguration getConfiguration()
    {
        return this.configuration;
    }

    /**
     *
     * @throws MojoExecutionException
     * @see com.atlassian.maven.plugin.clover.CloverInstrumentInternalMojo#calcIncludedFilesForGroovy()
     * @see com.atlassian.maven.plugin.clover.CloverInstrumentInternalMojo#redirectOutputDirectories()
     */
    public void instrument() throws MojoExecutionException {
        final CloverSourceScanner scanner = getSourceScanner();

        // get source files to be instrumented, but only for Java as they will be instrumented by CloverInstr
        final Map javaFilesToInstrument = scanner.getSourceFilesToInstrument(LanguageFileExtensionFilter.JAVA_LANGUAGE);
        if (javaFilesToInstrument.isEmpty()) {
            getConfiguration().getLog().info("No Clover instrumentation done on source files in: "
                    + getCompileSourceRoots() + " as no matching sources files found (JAVA_LANGUAGE)");
        } else {
            instrumentSources(javaFilesToInstrument, outputSourceDirectory);
        }

        // in case when 'src/main/java' (or 'src/test/java') contains *.groovy source files (this is a trick possible
        // with a groovy-eclipse-plugin, see http://groovy.codehaus.org/Groovy-Eclipse+compiler+plugin+for+Maven
        // "Setting up source folders / Do nothing") we must copy *.groovy files as well
        // reason: 'src/main/java' (or 'src/test/java') will be redirected to 'target/clover/src-instrumented'
        // (or 'target/clover/src-test-instrumented') and Groovy compiler must be able to find these groovy sources
        final Map/*<String,String[]>*/ groovyFilesToInstrument = scanner.getSourceFilesToInstrument(LanguageFileExtensionFilter.GROOVY_LANGUAGE);
        // don't copy files from 'src/main/groovy' and 'src/test/groovy' because these source roots are not being redirected
        GroovyMainSourceScanner.removeOwnSourceRoots(groovyFilesToInstrument.keySet());
        GroovyTestSourceScanner.removeOwnSourceRoots(groovyFilesToInstrument.keySet());
        if (!groovyFilesToInstrument.isEmpty()) {
            copyExcludedFiles(groovyFilesToInstrument, outputSourceDirectory);
        }

        // We need to copy excluded files too as otherwise they won't be in the new Clover source directory and
        // thus won't be compiled by the compile plugin. This will lead to compilation errors if any other
        // file depends on any of these excluded files.
        if (configuration.copyExcludedFiles()) {
            final Map/*<String,String[]>*/ explicitlyExcludedFiles = scanner.getExcludedFiles();
            // avoid having the same excluded file in two locations (in 'src/xxx/groovy' and 'src-xxx-instrumented')
            GroovyMainSourceScanner.removeOwnSourceRoots(explicitlyExcludedFiles.keySet());
            GroovyTestSourceScanner.removeOwnSourceRoots(explicitlyExcludedFiles.keySet());
            copyExcludedFiles(explicitlyExcludedFiles, outputSourceDirectory);
        }
    }

    public String redirectSourceDirectories()
    {
        return redirectSourceDirectories( outputSourceDirectory );
    }

    protected abstract CloverSourceScanner getSourceScanner();
    protected abstract String getSourceDirectory();
    protected abstract void setSourceDirectory(String targetDirectory);
    protected abstract List getCompileSourceRoots();
    protected abstract void addCompileSourceRoot(String sourceRoot);

    private String redirectSourceDirectories(String targetDirectory)
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
        List/*<String>*/ sourceRoots = new ArrayList/*<String>*/(getCompileSourceRoots());

        // Clean all source roots to add them again in order to keep the same original order of source roots.
        getCompileSourceRoots().removeAll(sourceRoots);

        for (Iterator/*<String>*/ i = sourceRoots.iterator(); i.hasNext(); ) {
            String sourceRoot = (String) i.next();
            if (new File(oldSourceDirectory).exists() && sourceRoot.equals(oldSourceDirectory)) {
                // add redirected location (e.g. 'src/main/java' -> 'target/clover/src-instrumented') instead of the
                // original source root
                addCompileSourceRoot(getSourceDirectory());
            }

            else if ( !(getConfiguration().includesAllSourceRoots() && isGeneratedSourcesDirectory(sourceRoot)) )
            {
                // if includeAllSourceRoots=true then ignore the original generated sources directory (e.g. target/generated/xyz),
                // because Clover will instrument them and store instrumented version (e.g. target/clover/src-instrumented);
                // compiler should know only the latter location, otherwise we would end up with the same classes included twice
                // (one with and one without Clover instrumentation)
                addCompileSourceRoot( sourceRoot );
            }
        }

        getConfiguration().getLog().debug( "Clover main source directories after change:" );
        logSourceDirectories();
        return oldSourceDirectory;
    }

    private boolean isGeneratedSourcesDirectory(String sourceRoot) {
        String generatedSrcDirDefaultLifecycle = File.separator + "target" + File.separator + "generated-sources";
        String generatedSrcDirCloverLifecycle = File.separator + "target" + File.separator + "clover" + File.separator + "generated-sources";
        return sourceRoot.indexOf(generatedSrcDirDefaultLifecycle) != -1
                || sourceRoot.indexOf(generatedSrcDirCloverLifecycle) != -1;
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
    private void copyExcludedFiles(Map/*<String,String[]*/ excludedFiles, String targetDirectory) throws MojoExecutionException {
        for (Iterator/*<String>*/ sourceRoots = excludedFiles.keySet().iterator(); sourceRoots.hasNext(); ) {
            String sourceRoot = (String) sourceRoots.next();
            String[] filesInSourceRoot = (String[]) excludedFiles.get( sourceRoot );

            for (int i = 0; i < filesInSourceRoot.length; i++) {

                File srcFile = new File(sourceRoot, filesInSourceRoot[i]);

                try
                {
                    configuration.getLog().debug("Copying excluded file: " + srcFile.getAbsolutePath() + " to " + targetDirectory );
                    FileUtils.copyFile(srcFile, new File( targetDirectory,
                        srcFile.getPath().substring(sourceRoot.length() ) ) );
                }
                catch (IOException e)
                {
                    throw new MojoExecutionException( "Failed to copy excluded file [" + srcFile + "] to ["
                        + targetDirectory + "]", e );
                }
            }
        }
    }

    private void instrumentSources( Map filesToInstrument, String outputDir ) throws MojoExecutionException {

        Logger.setInstance(new MvnLogger(configuration.getLog()));
        // only make dirs when there is src to instrument. see CLMVN-118
        new File(outputDir).mkdirs();
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
        parameters.add( getConfiguration().resolveCloverDatabase() );

        parameters.add( "-d" );
        parameters.add( outputDir );

        if ( getConfiguration().getLog().isDebugEnabled() )
        {
            parameters.add( "-v" );
        }

        if (getConfiguration().getDistributedCoverage() != null && getConfiguration().getDistributedCoverage().isEnabled()) {
            parameters.add("--distributedCoverage");
            parameters.add(getConfiguration().getDistributedCoverage().toString());
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
            else if ( getConfiguration().getJdk().equals( "1.7" ) )
            {
                parameters.add( "--source" );
                parameters.add( "1.7" );
            }
            else
            {
                throw new MojoExecutionException( "Unsupported jdk version [" + getConfiguration().getJdk()
                    + "]. Valid values are [1.4], [1.5], [1.6] and [1.7]" );
            }
        }

        if (!getConfiguration().isUseFullyQualifiedJavaLang())
        {
            parameters.add("--dontFullyQualifyJavaLang");
        }

        if (getConfiguration().getEncoding() != null) {
            parameters.add("--encoding");
            parameters.add(getConfiguration().getEncoding());
        } else if (getConfiguration().getProject().getProperties().get(PROP_PROJECT_BUILD_SOURCEENCODING) != null) {
            parameters.add("--encoding");
            parameters.add(getConfiguration().getProject().getProperties().get(PROP_PROJECT_BUILD_SOURCEENCODING));
        }

        if (getConfiguration().getInstrumentation() != null) {
            parameters.add("--instrlevel");
            parameters.add(getConfiguration().getInstrumentation());
        }

        for ( Iterator sourceRoots = filesToInstrument.keySet().iterator(); sourceRoots.hasNext(); )
        {
            final String srcDir = (String) sourceRoots.next();
            String[] filesInSourceRoot = (String[]) filesToInstrument.get(srcDir);
            for (int i = 0; i < filesInSourceRoot.length; i++) {
                String s = filesInSourceRoot[i];

                File file = new File(srcDir, s);
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

