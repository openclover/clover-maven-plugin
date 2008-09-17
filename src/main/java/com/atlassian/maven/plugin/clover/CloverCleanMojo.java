package com.atlassian.maven.plugin.clover;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.clean.CleanMojo;
import org.apache.maven.plugin.clean.Fileset;
import org.apache.maven.shared.model.fileset.FileSet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.AccessibleObject;
import java.io.File;

/**
 * @goal clean
 * @phase initialize
 */
public class CloverCleanMojo extends CleanMojo {


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

    /**
     * This is where build results go.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File directory;

    /**
     * This is where compiled classes go.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * This is where compiled test classes go.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    private File testOutputDirectory;

    /**
     * This is where the site plugin generates its pages.
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @required
     * @readonly
     * @since 2.1.1
     */
    private File reportDirectory;

    /**
     * The comma-delimited includes patterns to use when deleting the default directory locations.
     *
     * @parameter expression="${maven.clean.includes}"
     * @since 2.3
     */

    public void execute() throws MojoExecutionException {
        // delete just the checkpoint
        setPrivateField("excludeDefaultDirectories", true);
        super.execute();

        removeFiltered(directory.getPath());
        removeFiltered(outputDirectory.getPath());
        removeFiltered(testOutputDirectory.getPath());
        removeFiltered(reportDirectory.getPath());
    }

    private void removeFiltered(String path) throws MojoExecutionException {
        removeFileSet(path);

    }

    private void removeFileSet(String path) throws MojoExecutionException {
        FileSet fileset = new Fileset();
        fileset.setDirectory(path);
        fileset.addExclude(cloverCheckpoint);
        invokePrivateMethod("removeFileSet", fileset);
    }

    private void invokePrivateMethod(String name, Object param) throws MojoExecutionException {
        try {
            final Class superclass = getSuperClass();
            Method method = superclass.getDeclaredMethod(name, new Class[]{FileSet.class});
            method.setAccessible(true);
            method.invoke(this, new Object[]{param});

        } catch (IllegalAccessException e) {
            getLog().error("Could not get: " + name, e);
        } catch (NoSuchMethodException e) {
            getLog().error("Could not get: " + name, e);
        } catch (InvocationTargetException e) {
            getLog().error("Could not get: " + name, e);
        }
    }

    private Class getSuperClass() {
        final Class superclass = CleanMojo.class;
        AccessibleObject.setAccessible(superclass.getDeclaredFields(), true);
        return superclass;
    }

    private void setPrivateField(String fieldName, boolean value) throws MojoExecutionException {
        try {
            final Class superclass = getSuperClass();
            Field field = superclass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setBoolean(this, value);

        } catch (NoSuchFieldException e) {
            getLog().error("Could not set: " + fieldName, e);
        } catch (IllegalAccessException e) {
            getLog().error("Could not set: " + fieldName, e);
        }
    }
}
