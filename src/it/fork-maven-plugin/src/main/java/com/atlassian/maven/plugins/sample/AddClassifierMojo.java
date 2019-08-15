package com.atlassian.maven.plugins.sample;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.component.annotations.Requirement;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

/**
 * Change artifact classifier to 'fork'.
 */
@Mojo(name = "add-classifier")
public class AddClassifierMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    @Requirement
    private RepositorySystem repositorySystem;

    public void execute() {
        final Artifact oldArtifact = project.getArtifact();
        final Artifact newArtifact = repositorySystem.createArtifactWithClassifier(
                oldArtifact.getGroupId(),
                oldArtifact.getArtifactId(),
                oldArtifact.getVersion(),
                oldArtifact.getType(),
                "fork");
        project.setArtifact(newArtifact);
    }
}
