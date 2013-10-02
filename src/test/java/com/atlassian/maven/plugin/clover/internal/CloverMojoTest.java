package com.atlassian.maven.plugin.clover.internal;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Model;
import org.jmock.Mockery;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;

import java.io.File;

import com.atlassian.clover.CloverNames;
import static org.hamcrest.Matchers.startsWith;

/**
 * Unit tests for {@link com.atlassian.maven.plugin.clover.internal.AbstractCloverMojo}.
 * 
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 */
public class CloverMojoTest extends MockObjectTestCase
{
    private MavenProject dummyProject;

    protected void setUp() throws Exception {
        super.setUp();
        dummyProject = new MavenProject((Model) null) {

            public File getFile() {
                return new File("./pom.xml");
            }
        };
    }

    public class TestableAbstractCloverMojo extends AbstractCloverMojo
    {
        public void execute() throws MojoExecutionException
        {
            // Voluntarily left blank
        }
    }

    public void testRegisterLicenseFile() throws MojoExecutionException, FileResourceCreationException, ResourceNotFoundException {
        TestableAbstractCloverMojo mojo = new TestableAbstractCloverMojo();
        Mockery context = new Mockery();        

        final ResourceManager mockResourceManager = context.mock( ResourceManager.class, "resource manager" );

            context.checking(new Expectations(){{
                oneOf(mockResourceManager).addSearchPath(FileResourceLoader.ID, dummyProject.getFile().getParentFile().getAbsolutePath());
                oneOf(mockResourceManager).addSearchPath("url", "");
                oneOf(mockResourceManager).getResourceAsFile(with("build-tools/clover.license"),
                                                             with(startsWith(System.getProperty("java.io.tmpdir"))));
                will(returnValue(new File("targetFile")));

            }});
        mojo.setResourceManager( mockResourceManager );

        // Ensure that the system property is not already set
        System.setProperty( CloverNames.PROP_LICENSE_PATH, "" );

        try {
            mojo.setLicenseLocation( "build-tools/clover.license" );

            mojo.setProject(dummyProject);
            mojo.registerLicenseFile();
            context.assertIsSatisfied();

            assertEquals( "targetFile", System.getProperty( CloverNames.PROP_LICENSE_PATH ) );
        } finally {
            System.getProperties().remove( CloverNames.PROP_LICENSE_PATH );            
        }
    }

    public void testRegisterLicense() throws MojoExecutionException
    {
        TestableAbstractCloverMojo mojo = new TestableAbstractCloverMojo();
        // Ensure that the system property is not already set
        System.setProperty( CloverNames.PROP_LICENSE_CERT, "" );

        final String license = "fu11l1c3nc3str1ngg03sh3r3\n" +
                               "w1thn3wl1n3s4ndf0rm4tt1ng.";
        mojo.setLicense(license);

        try {
            mojo.setProject(dummyProject);
            mojo.registerLicenseFile();
            assertNull("", System.getProperty(CloverNames.PROP_LICENSE_PATH) );
            assertEquals(license, System.getProperty(CloverNames.PROP_LICENSE_CERT) );
        } finally {
            System.getProperties().remove( CloverNames.PROP_LICENSE_PATH );
            System.getProperties().remove( CloverNames.PROP_LICENSE_CERT);            

        }


    }

}
