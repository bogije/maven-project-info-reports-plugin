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
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;

/**
 * @author Robert Scholte
 * @since 3.0.0
 */
@Component( role = WagonRepositoryConnectorFactory.class, hint = "maven3" )
public class Maven31WagonRepositoryConnectorFactory
{
    @Requirement( role = WagonRepositoryConnectorFactory.class, hint = "wagon" )
    private WagonRepositoryConnectorFactory wrcf;

    public Maven31WagonRepositoryConnector newInstance( ProjectBuildingRequest request, ArtifactRepository repository )
        throws WagonRepositoryConnectorException
    {
        try
        {
            RepositorySystemSession session =
                (RepositorySystemSession) Invoker.invoke( request, "getRepositorySession" );

            RemoteRepository remoteRepository = getRemoteRepository( session, repository );

            RepositoryConnector repoConnector = wrcf.newInstance( session, remoteRepository );

            return new Maven31WagonRepositoryConnector( repoConnector );
        }
        catch ( NoRepositoryConnectorException e )
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

        if ( aetherRepo.getAuthentication() == null || aetherRepo.getProxy() == null )
        {
            RemoteRepository.Builder builder = new RemoteRepository.Builder( aetherRepo );

            if ( aetherRepo.getAuthentication() == null )
            {
                builder.setAuthentication( session.getAuthenticationSelector().getAuthentication( aetherRepo ) );
            }

            if ( aetherRepo.getProxy() == null )
            {
                builder.setProxy( session.getProxySelector().getProxy( aetherRepo ) );
            }

            aetherRepo = builder.build();
        }

        return aetherRepo;
    }
}
