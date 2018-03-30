package org.apache.maven.report.projectinfo.wagon.internal;

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

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.report.projectinfo.wagon.WagonRepositoryConnectorException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.transfer.NoRepositoryConnectorException;

/**
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
@Component( role = WagonRepositoryConnectorFactory.class, hint = "maven3" )
public class Maven30WagonRepositoryConnectorFactory
{
    @Requirement( role = WagonRepositoryConnectorFactory.class, hint = "wagon" )
    private WagonRepositoryConnectorFactory wrcf;

    public Maven30WagonRepositoryConnector newInstance( ProjectBuildingRequest request, ArtifactRepository repository )
        throws WagonRepositoryConnectorException
    {
        try
        {
            RemoteRepository remoteRepository = getRemoteRepository( request.getRepositorySession(), repository );

            RepositoryConnector repoConnector = wrcf.newInstance( request.getRepositorySession(), remoteRepository );

            return new Maven30WagonRepositoryConnector( repoConnector );
        }
        catch ( NoRepositoryConnectorException e )
        {
            throw new WagonRepositoryConnectorException( e.getMessage(), e );
        }
        catch ( WagonRepositoryConnectorException e )
        {
            throw new WagonRepositoryConnectorException( e.getMessage(), e );
        }
    }

    private RemoteRepository getRemoteRepository( RepositorySystemSession session, ArtifactRepository remoteRepository )
        throws WagonRepositoryConnectorException
    {
        // CHECKSTYLE_OFF: LineLength
        RemoteRepository aetherRepo =
            (RemoteRepository) Invoker.invoke( RepositoryUtils.class, "toRepo",
                                               org.apache.maven.artifact.repository.ArtifactRepository.class,
                                               remoteRepository );
        // CHECKSTYLE_ON: LineLength

        if ( aetherRepo.getAuthentication() == null )
        {
            aetherRepo.setAuthentication( session.getAuthenticationSelector().getAuthentication( aetherRepo ) );
        }

        if ( aetherRepo.getProxy() == null )
        {
            aetherRepo.setProxy( session.getProxySelector().getProxy( aetherRepo ) );
        }

        return aetherRepo;
    }
}
