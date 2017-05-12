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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.logging.Log;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.jmock.Expectations;

import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.io.File;

/**
 * Unit tests for {@link com.atlassian.maven.plugin.clover.CloverInstrumentInternalMojo}.
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 */
public class CloverInstrumentInternalMojoTest extends MockObjectTestCase {
    private CloverInstrumentInternalMojo mojo;

    /**
     * Class used to return a given value when lastModified is called. This is because File.getLastModified always
     * return 0L if the file doesn't exist and our tests below do not point to existing files.
     */
    public class MockFile extends File {
        private long lastModifiedDate;

        public MockFile(final String file, final long lastModifiedDate) {
            super(file);
            this.lastModifiedDate = lastModifiedDate;
        }

        public long lastModified() {
            return this.lastModifiedDate;
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        this.mojo = new CloverInstrumentInternalMojo();
    }

    public void testFindCloverArtifactWithCorrectArtifactIdButWrongGroupId() {
        final Artifact mockArtifact = mock(Artifact.class);
        checking(new Expectations() {{
            oneOf(mockArtifact).getGroupId();
            will(returnValue("notcenquaid"));
        }});

        Artifact clover = this.mojo.findCloverArtifact(Collections.singletonList(mockArtifact));

        assertNull("Clover artifact should not have been found!", clover);
    }

    public void testFindCloverArtifactWhenCorrectIds() {

        final Artifact mockArtifact = mock(Artifact.class);
        checking(new Expectations() {{
            oneOf(mockArtifact).getArtifactId();
            will(returnValue("clover"));
            oneOf(mockArtifact).getGroupId();
            will(returnValue("com.atlassian.clover"));
        }});

        Artifact clover = this.mojo.findCloverArtifact(Collections.singletonList(mockArtifact));
        assertNotNull("Clover artifact should have been found!", clover);
    }


    public void testSwizzleCloverDependenciesWhenDependencyHasClassifier() {
        final Artifact artifact = setUpMockArtifact("some.groupId", "someArtifactId", "1.0", "jar", "compile", "whatever",
                null);

        final Set<Artifact> resultSet = this.mojo.swizzleCloverDependencies(Collections.singleton(artifact));
        assertEquals(1, resultSet.size());
        assertTrue("Resulting artifact should have been the original one", resultSet.contains(artifact));
    }

    public void testSwizzleCloverDependenciesWhenCloveredVersionOfDependencyIsNewerThanOriginal() throws ArtifactNotFoundException, ArtifactResolutionException {
        // Ensure that the original artifact is older than the clovered artifact so that the clovered artifact
        // is picked. Note that that we use -5000/-10000 to ensure not to set the time in the future as maybe
        // this could cause some problems on some OS.
        long now = System.currentTimeMillis();
        final File artifactFile = new MockFile("some/file/artifact", now - 10000L);
        final File cloveredArtifactFile = new MockFile("some/file/cloveredArtifact", now - 5000L);

        final Artifact artifact = setUpMockArtifact("some.groupId", "someArtifactId", "1.0", "jar", "compile", null,
                artifactFile);
        final Artifact cloveredArtifact = setUpMockArtifact(null, null, null, null, null, null, cloveredArtifactFile);

        setUpCommonMocksForSwizzleCloverDependenciesTests(cloveredArtifact);
        checking(new Expectations() {{
            oneOf(cloveredArtifact).setScope("compile");
        }});

        final Set<Artifact> resultSet = this.mojo.swizzleCloverDependencies(Collections.singleton(artifact));
        assertEquals(1, resultSet.size());
        assertTrue("Resulting artifact should have been the clovered one", resultSet.contains(cloveredArtifact));
    }

    public void testSwizzleCloverDependenciesWhenOriginalVersionOfDependencyIsNewerThanCloveredOne() throws ArtifactNotFoundException, ArtifactResolutionException {
        // Ensure that the clovered artifact is older than the original artifact so that the original artifact
        // is picked. Note that that we use -5000/-10000 to ensure not to set the time in the future as maybe
        // this could cause some problems on some OS.
        long now = System.currentTimeMillis();
        final File artifactFile = new MockFile("some/file/artifact", now - 5000L);
        final File cloveredArtifactFile = new MockFile("some/file/cloveredArtifact", now - 10000L);

        final Artifact artifact = setUpMockArtifact("some.groupId", "someArtifactId", "1.0", "jar", "compile", null,
                artifactFile);
        final Artifact cloveredArtifact = setUpMockArtifact(null, null, null, null, null, null, cloveredArtifactFile);
        checking(new Expectations() {{
            oneOf(cloveredArtifact).setScope("compile");
        }});

        setUpCommonMocksForSwizzleCloverDependenciesTests(cloveredArtifact);

        final Set<Artifact> resultSet = this.mojo.swizzleCloverDependencies(Collections.singleton(artifact));
        assertEquals(1, resultSet.size());
        assertTrue("Resulting artifact should have been the original one", resultSet.contains(artifact));
    }


    private void setUpCommonMocksForSwizzleCloverDependenciesTests(final Artifact artifact) throws ArtifactNotFoundException, ArtifactResolutionException {
        final ArtifactFactory mockArtifactFactory = mock(ArtifactFactory.class);
        checking(new Expectations() {{
            oneOf(mockArtifactFactory).createArtifactWithClassifier(
                    "some.groupId", "someArtifactId", "1.0", "jar", "clover");
            will(returnValue(artifact));
        }});


        final ArtifactResolver mockArtifactResolver = mock(ArtifactResolver.class);
        checking(new Expectations() {{
            oneOf(mockArtifactResolver).resolve(with(any(Artifact.class)), with(any(List.class)), with(any(ArtifactRepository.class)));
        }});

        final Log mockLog = mock(Log.class);
        checking(new Expectations() {{
            atLeast(0).of(mockLog).warn(with(any(String.class)));
        }});

        this.mojo.setArtifactFactory(mockArtifactFactory);
        this.mojo.setArtifactResolver(mockArtifactResolver);
        this.mojo.setLog(mockLog);
    }

    private Artifact setUpMockArtifact(final String groupId, final String artifactId,
                                       final String version, final String type,
                                       final String scope, final String classifier,
                                       final File file) {
        final Artifact mockArtifact = mock(Artifact.class, artifactId);
        checking(new Expectations() {{
            atLeast(0).of(mockArtifact).getClassifier();
            will(returnValue(classifier));
            atLeast(0).of(mockArtifact).getGroupId();
            will(returnValue(groupId));
            atLeast(0).of(mockArtifact).getArtifactId();
            will(returnValue(artifactId));
            atLeast(0).of(mockArtifact).getVersion();
            will(returnValue(version));
            atLeast(0).of(mockArtifact).getType();
            will(returnValue(type));
            atLeast(0).of(mockArtifact).getScope();
            will(returnValue(scope));
            atLeast(0).of(mockArtifact).getFile();
            will(returnValue(file));
            atLeast(0).of(mockArtifact).getId();
            will(returnValue(groupId + ":" + artifactId + ":" + version + ":" + classifier));
        }});
        return mockArtifact;
    }
}
