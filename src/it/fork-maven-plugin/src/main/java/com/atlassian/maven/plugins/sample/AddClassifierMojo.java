package com.atlassian.maven.plugins.sample;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Change artifact classifier to 'fork'.
 *
 * @goal add-classifier
 */
public class AddClassifierMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    public void execute() throws MojoExecutionException {
        Artifact oldArtifact = project.getArtifact();
        Artifact newArtifact = this.artifactFactory.createArtifactWithClassifier(oldArtifact.getGroupId(),
                oldArtifact.getArtifactId(), oldArtifact.getVersion(), oldArtifact.getType(), "fork");
        project.setArtifact(newArtifact);
    }
}
