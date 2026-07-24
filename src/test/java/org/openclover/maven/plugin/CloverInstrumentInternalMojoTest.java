package org.openclover.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CloverInstrumentInternalMojo}.
 */
public class CloverInstrumentInternalMojoTest {
    private CloverInstrumentInternalMojo mojo;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    /** Projects making up the current reactor build; empty unless a test registers a module. */
    private List<MavenProject> reactorProjects = Collections.emptyList();

    /**
     * Class used to return a given value when lastModified is called. This is because File.getLastModified always
     * return 0L if the file doesn't exist and our tests below do not point to existing files.
     */
    private static class MockFile extends File {
        private final long lastModifiedDate;

        public MockFile(final String file, final long lastModifiedDate) {
            super(file);
            this.lastModifiedDate = lastModifiedDate;
        }

        public long lastModified() {
            return lastModifiedDate;
        }
    }

    @Before
    public void setUp() {
        this.mojo = new CloverInstrumentInternalMojo();
        CloverInstrumentInternalMojo.clearInstrumentedOutputDirectories();
    }

    @Test
    public void testFindCloverArtifactWithCorrectArtifactIdButWrongGroupId() {
        final Artifact mockArtifact = mock(Artifact.class);
        when(mockArtifact.getGroupId()).thenReturn("not.org.openclover");

        Artifact clover = this.mojo.findCloverArtifact(Collections.singletonList(mockArtifact));

        assertNull("Clover artifact should not have been found!", clover);
    }

    @Test
    public void testFindCloverArtifactWhenCorrectIds() {
        final Artifact mockArtifact = mock(Artifact.class);
        when(mockArtifact.getGroupId()).thenReturn("org.openclover");
        when(mockArtifact.getArtifactId()).thenReturn("clover");

        Artifact clover = this.mojo.findCloverArtifact(Collections.singletonList(mockArtifact));

        assertNotNull("Clover artifact should have been found!", clover);
    }

    @Test
    public void testSwizzleCloverDependenciesWhenDependencyHasClassifier() {
        final Artifact artifact = setUpMockArtifact("some.groupId", "someArtifactId", "1.0", "jar", "compile", "whatever",
                null);

        final Set<Artifact> resultSet = this.mojo.swizzleCloverDependencies(Collections.singleton(artifact));
        assertThat(resultSet.size(), equalTo(1));
        assertTrue("Resulting artifact should have been the original one", resultSet.contains(artifact));
    }

    @Test
    public void testSwizzleCloverDependenciesWhenCloveredVersionOfDependencyIsNewerThanOriginal() throws ArtifactResolverException {
        // Ensure that the original artifact is older than the clovered artifact so that the clovered artifact
        // is picked. Note that we use -5000/-10000 to ensure not to set the time in the future as maybe
        // this could cause some problems on some OS.
        long now = System.currentTimeMillis();
        final File artifactFile = new MockFile("some/file/artifact", now - 10000L);
        final File cloveredArtifactFile = new MockFile("some/file/cloveredArtifact", now - 5000L);
        final Artifact artifact = setUpMockArtifact("some.groupId", "someArtifactId", "1.0",
                "jar", "compile", null, artifactFile);
        final Artifact cloveredArtifact = setUpMockArtifact(null, null, null,
                null, null, null, cloveredArtifactFile);

        final Log mockLog = mock(Log.class);
        this.mojo.setLog(mockLog);
        final MavenExecutionRequest mockExecutionRequest = mock(MavenExecutionRequest.class);
        setUpCommonMocksForSwizzleCloverDependenciesTests(cloveredArtifact, mockExecutionRequest);

        final Set<Artifact> resultSet = this.mojo.swizzleCloverDependencies(Collections.singleton(artifact));

        verify(cloveredArtifact, atLeastOnce()).setScope("compile");
        verify(mockLog, never()).warn(any(String.class));
        verify(mockLog, never()).warn(matches("Using .*, built on .* even though a Clovered version exists but it's older"));

        assertEquals(1, resultSet.size());
        assertTrue("Resulting artifact should have been the clovered one", resultSet.contains(cloveredArtifact));
    }

    @Test
    public void testSwizzleCloverDependenciesWhenOriginalVersionOfDependencyIsNewerThanCloveredOne() throws ArtifactResolverException {
        // Ensure that the clovered artifact is older than the original artifact so that the original artifact
        // is picked. Note that we use -5000/-10000 to ensure not to set the time in the future as maybe
        // this could cause some problems on some OS.
        long now = System.currentTimeMillis();
        final File artifactFile = new MockFile("some/file/artifact", now - 5000L);
        final File cloveredArtifactFile = new MockFile("some/file/cloveredArtifact", now - 10000L);
        final Artifact artifact = setUpMockArtifact("some.groupId", "someArtifactId", "1.0",
                "jar", "compile", null, artifactFile);
        final Artifact cloveredArtifact = setUpMockArtifact(null, null, null,
                null, null, null, cloveredArtifactFile);

        final Log mockLog = mock(Log.class);
        this.mojo.setLog(mockLog);
        final MavenExecutionRequest mockExecutionRequest = mock(MavenExecutionRequest.class);
        setUpCommonMocksForSwizzleCloverDependenciesTests(cloveredArtifact, mockExecutionRequest);

        final Set<Artifact> resultSet = this.mojo.swizzleCloverDependencies(Collections.singleton(artifact));

        verify(cloveredArtifact, atLeastOnce()).setScope("compile");
        verify(mockLog, atLeastOnce()).warn(any(String.class));
        verify(mockLog, atLeastOnce()).warn(matches("Using .*, built on .* even though a Clovered version exists but it's older"));

        assertEquals(1, resultSet.size());
        assertTrue("Resulting artifact should have been the original one", resultSet.contains(artifact));
    }

    /**
     * A dependency on a module which is being built (and instrumented) in the same session must be taken from
     * the reactor and not from the local repository.
     */
    @Test
    public void testSwizzleCloverDependenciesWhenDependencyIsAModuleOfTheCurrentBuild() throws Exception {
        final File instrumentedClasses = temporaryFolder.newFolder("moduleA", "target", "clover", "classes");
        final File instrumentedTestClasses = temporaryFolder.newFolder("moduleA", "target", "clover", "test-classes");
        final Artifact moduleArtifact = registerInstrumentedModule(instrumentedClasses, instrumentedTestClasses);

        // a plain jar dependency on that module - must resolve to the main instrumented classes
        final Artifact cloveredArtifact = setUpMockArtifact(null, null, null,
                null, null, null, new MockFile("some/file/cloveredArtifact", System.currentTimeMillis()));
        final Artifact dependency = setUpMockArtifact("some.groupId", "someArtifactId", "1.0",
                "jar", "compile", null, new MockFile("some/file/artifact", 0L));
        setUpCommonMocksForSwizzleCloverDependenciesTests(cloveredArtifact, mock(MavenExecutionRequest.class));

        final Set<Artifact> resultSet = this.mojo.swizzleCloverDependencies(Collections.singleton(dependency));

        assertEquals(1, resultSet.size());
        assertTrue("Resulting artifact should have been the original one", resultSet.contains(dependency));
        // both the dependency and the module of the reactor must point to the main instrumented classes
        verify(dependency).setFile(instrumentedClasses);
        verify(moduleArtifact).setFile(instrumentedClasses);
    }

    /**
     * A 'test-jar' dependency on a module of the current build must resolve to that module's instrumented
     * test classes, not to its main classes and not to a stale artifact from the local repository.
     */
    @Test
    public void testSwizzleCloverDependenciesWhenDependencyIsATestJarOfAModuleOfTheCurrentBuild() throws Exception {
        final File instrumentedClasses = temporaryFolder.newFolder("moduleA", "target", "clover", "classes");
        final File instrumentedTestClasses = temporaryFolder.newFolder("moduleA", "target", "clover", "test-classes");
        registerInstrumentedModule(instrumentedClasses, instrumentedTestClasses);

        // a test-jar dependency on that module
        final Artifact dependency = setUpMockArtifact("some.groupId", "someArtifactId", "1.0",
                "test-jar", "test", null, new MockFile("some/file/testArtifact", 0L));
        setUpCommonMocksForSwizzleCloverDependenciesTests(
                mock(Artifact.class), mock(MavenExecutionRequest.class));

        final Set<Artifact> resultSet = this.mojo.swizzleCloverDependencies(Collections.singleton(dependency));

        assertEquals(1, resultSet.size());
        assertTrue("Resulting artifact should have been the original one", resultSet.contains(dependency));
        // the test-jar dependency must point to the instrumented TEST classes, not the main ones
        verify(dependency).setFile(instrumentedTestClasses);
    }

    /**
     * Registers a module of the current build (with its main and test instrumented output directories) and wires
     * up the session so that it is discoverable as a reactor project. Returns the module's project artifact.
     */
    private Artifact registerInstrumentedModule(final File mainClasses, final File testClasses) {
        final Artifact moduleArtifact = setUpMockArtifact("some.groupId", "someArtifactId", "1.0",
                "jar", "compile", null, null);
        final MavenProject module = mock(MavenProject.class);
        when(module.getGroupId()).thenReturn("some.groupId");
        when(module.getArtifactId()).thenReturn("someArtifactId");
        when(module.getVersion()).thenReturn("1.0");
        when(module.getPackaging()).thenReturn("jar");
        when(module.getArtifact()).thenReturn(moduleArtifact);
        final Build build = new Build();
        build.setOutputDirectory(mainClasses.getAbsolutePath());
        build.setTestOutputDirectory(testClasses.getAbsolutePath());
        when(module.getBuild()).thenReturn(build);

        this.mojo.setLog(mock(Log.class));
        this.mojo.setProject(module);
        this.mojo.registerInstrumentedOutputDirectory();

        this.reactorProjects = Collections.singletonList(module);
        return moduleArtifact;
    }

    private MavenSession setUpCommonMocksForSwizzleCloverDependenciesTests(final Artifact artifact,
                                                                           final MavenExecutionRequest mockExecutionRequest) throws ArtifactResolverException {
        final RepositorySystem mockRepositorySystem = mock(RepositorySystem.class);
        when(mockRepositorySystem.createArtifactWithClassifier("some.groupId", "someArtifactId", "1.0", "jar", "clover"))
                .thenReturn(artifact);

        final ArtifactResult artifactResult = mock(ArtifactResult.class);
        when(artifactResult.getArtifact()).thenReturn(artifact);

        final ArtifactResolver mockArtifactResolver = mock(ArtifactResolver.class);
        when(mockArtifactResolver.resolveArtifact(any(ProjectBuildingRequest.class), any(Artifact.class)))
                .thenReturn(artifactResult);

        final MavenSession mockMavenSession = mock(MavenSession.class);
        when(mockMavenSession.getRequest())
                .thenReturn(mockExecutionRequest);
        when(mockMavenSession.getProjectBuildingRequest())
                .thenReturn(new DefaultProjectBuildingRequest());
        when(mockMavenSession.getProjects())
                .thenReturn(this.reactorProjects);

        this.mojo.repositorySystem = mockRepositorySystem;
        this.mojo.artifactResolver = mockArtifactResolver;
        this.mojo.setMavenSession(mockMavenSession);
        return mockMavenSession;
    }

    private static Artifact setUpMockArtifact(final String groupId, final String artifactId,
                                              final String version, final String type,
                                              final String scope, final String classifier,
                                              final File file) {
        final Artifact mockArtifact = mock(Artifact.class, artifactId);

        when(mockArtifact.getClassifier()).thenReturn(classifier);
        when(mockArtifact.hasClassifier()).thenReturn(classifier != null);
        when(mockArtifact.getGroupId()).thenReturn(groupId);
        when(mockArtifact.getArtifactId()).thenReturn(artifactId);
        when(mockArtifact.getVersion()).thenReturn(version);
        when(mockArtifact.getType()).thenReturn(type);
        when(mockArtifact.getScope()).thenReturn(scope);
        when(mockArtifact.getFile()).thenReturn(file);
        when(mockArtifact.getId()).thenReturn(groupId + ":" + artifactId + ":" + version + ":" + classifier);

        return mockArtifact;
    }
}
