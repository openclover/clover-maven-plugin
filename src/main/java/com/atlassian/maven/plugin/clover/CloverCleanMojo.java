package com.atlassian.maven.plugin.clover;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.clean.Fileset;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @extendsPlugin clean
 * @extendsGoal clean
 * @goal clean
 * @phase initialize
 */
public class CloverCleanMojo extends AbstractMojo {


    /**
     * The location of the <a href="http://confluence.atlassian.com/x/EIBOB">Clover database</a>.
     *
     * @parameter expression="${maven.clover.cloverDatabase}" default-value="${project.build.directory}/clover/clover.db"
     */
    private String cloverDatabase;

    /**
     * The location of the Checkpoint file. By default, this is next to the cloverDatabase.
     *
     * @parameter expression="${maven.clover.cloverCheckpoint}" default-value="**\/*.teststate"
     */
    private String cloverCheckpoint;

    public void execute() throws MojoExecutionException {

        getLog().info("Excluding: " + cloverCheckpoint + " ");
        Fileset fileset = new Fileset();
        fileset.setUseDefaultExcludes(false);
        fileset.setDirectory(project.getBuild().getDirectory());
        fileset.addExclude(cloverCheckpoint);
        addFileset(fileset);
        executeClean();
    }



    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     * @since 2.2
     */
    private MavenProject project;

    /**
     * This is where build results go.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File directory;

    /**
     * This is where compiled classes go.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * This is where compiled test classes go.
     *
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    private File testOutputDirectory;

    /**
     * This is where the site plugin generates its pages.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     * @readonly
     * @since 2.1.1
     */
    private File reportDirectory;

    /**
     * Sets whether the plugin runs in verbose mode.
     *
     * @parameter expression="${clean.verbose}" default-value="false"
     * @since 2.1
     */
    private boolean verbose;

    /**
     * The list of fileSets to delete, in addition to the default directories.
     *
     * @parameter
     * @since 2.1
     */
    private List filesets;

    /**
     * Sets whether the plugin should follow Symbolic Links to delete files.
     *
     * @parameter expression="${clean.followSymLinks}" default-value="false"
     * @since 2.1
     */
    private boolean followSymLinks;

    /**
     * Finds and retrieves included and excluded files, and handles their
     * deletion
     *
     * @since 2.1
     */
    private FileSetManager fileSetManager;

    /**
     * Disable the plugin execution.
     *
     * @parameter expression="${clean.skip}" default-value="false"
     * @since 2.2
     */
    private boolean skip;

    /**
     * Indicates whether the build will continue even if there are clean errors.
     *
     * @parameter expression="${maven.clean.failOnError}" default-value="true"
     * @since 2.2
     */
    private boolean failOnError;

    /**
     * Deletes file-sets in the following project build directory order:
     * (source) directory, output directory, test directory, report directory,
     * and then the additional file-sets.
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     * @throws MojoExecutionException When a directory failed to get deleted.
     */
    public void executeClean()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Clean is skipped." );
            return;
        }

        try
        {
            fileSetManager = new FileSetManager( getLog(), verbose );
            removeAdditionalFilesets();
        }
        catch ( MojoExecutionException e )
        {
            if ( failOnError )
            {
                throw e;
            }

            getLog().warn( e.getMessage() );
        }
    }

    /**
     * Deletes additional file-sets specified by the <code>filesets</code> tag.
     *
     * @throws MojoExecutionException When a directory failed to get deleted.
     */
    private void removeAdditionalFilesets()
        throws MojoExecutionException
    {
        if ( filesets != null && !filesets.isEmpty() )
        {
            for ( Iterator it = filesets.iterator(); it.hasNext(); )
            {
                Fileset fileset = (Fileset) it.next();

                try
                {
                    getLog().info( "Deleting " + fileset );

                    if ( !project.isExecutionRoot() )
                    {
                        String projectBasedir = StringUtils.replace( project.getBasedir().getAbsolutePath(),
                                                                     "\\", "/" );
                        String filesetDir = StringUtils.replace( fileset.getDirectory(), "\\", "/" );

                        if ( filesetDir.indexOf( projectBasedir ) == -1 )
                        {
                            fileset.setDirectory( projectBasedir + "/" + filesetDir );
                        }
                    }

                    fileSetManager.delete( fileset, failOnError );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Failed to delete directory: " + fileset.getDirectory()
                        + ". Reason: " + e.getMessage(), e );
                }
            }
        }
    }

    /**
     * Deletes a directory and its contents.
     *
     * @param dir The base directory of the included and excluded files.
     * @throws MojoExecutionException When a directory failed to get deleted.
     */
    private void removeDirectory( File dir )
        throws MojoExecutionException
    {
        if ( dir != null )
        {
            if ( !dir.exists() )
            {
                return;
            }

            if ( !dir.isDirectory() )
            {
                throw new MojoExecutionException( dir + " is not a directory." );
            }

            FileSet fs = new FileSet();
            fs.setDirectory( dir.getPath() );
            fs.addInclude( "**/**" );
            fs.setFollowSymlinks( followSymLinks );

            try
            {
                getLog().info( "Deleting directory " + dir.getAbsolutePath() );
                fileSetManager.delete( fs, failOnError );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to delete directory: " + dir + ". Reason: " + e.getMessage(),
                                                  e );
            }
            catch ( IllegalStateException e )
            {
                // TODO: IOException from plexus-utils should be acceptable here
                throw new MojoExecutionException( "Failed to delete directory: " + dir + ". Reason: " + e.getMessage(),
                                                  e );
            }
        }
    }

    /**
     * Sets the project build directory.
     *
     * @param newDirectory The project build directory to set.
     */
    protected void setDirectory( File newDirectory )
    {
        this.directory = newDirectory;
    }

    /**
     * Sets the project build output directory.
     *
     * @param newOutputDirectory The project build output directory to set.
     */
    protected void setOutputDirectory( File newOutputDirectory )
    {
        this.outputDirectory = newOutputDirectory;
    }

    /**
     * Sets the project build test output directory.
     *
     * @param newTestOutputDirectory The project build test output directory to set.
     */
    protected void setTestOutputDirectory( File newTestOutputDirectory )
    {
        this.testOutputDirectory = newTestOutputDirectory;
    }

    /**
     * Sets the project build report directory.
     *
     * @param newReportDirectory The project build report directory to set.
     */
    protected void setReportDirectory( File newReportDirectory )
    {
        this.reportDirectory = newReportDirectory;
    }

    /**
     * Adds a file-set to the list of file-sets to clean.
     *
     * @param fileset the fileset
     */
    public void addFileset( Fileset fileset )
    {
        if ( filesets == null )
        {
            filesets = new LinkedList();
        }
        filesets.add( fileset );
    }

}
