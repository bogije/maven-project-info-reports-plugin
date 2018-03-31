package org.apache.maven.report.projectinfo.dependencies;

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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.report.projectinfo.wagon.WagonRepositoryConnector;
import org.apache.maven.report.projectinfo.wagon.WagonRepositoryConnectorException;
import org.apache.maven.report.projectinfo.wagon.WagonRepositoryConnectorFactory;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Utilities methods to play with repository
 *
 * @since 2.1
 */
public class RepositoryUtils
{
    private static final List<String> UNKNOWN_HOSTS = new ArrayList<String>();

    private final Log log;

    private final WagonRepositoryConnectorFactory wagonRepositoryConnectorFactory;

    private final ProjectBuilder projectBuilder;

    private final RepositorySystem repositorySystem;

    private final List<ArtifactRepository> remoteRepositories;

    private final List<ArtifactRepository> pluginRepositories;

    private final ArtifactResolver resolver;

    private final ProjectBuildingRequest buildingRequest;

    /**
     * @param log {@link Log}
     * @param projectBuilder {@link ProjectBuilder}
     * @param repositorySystem {@link RepositorySystem}
     * @param resolver {@link ArtifactResolver}
     * @param remoteRepositories {@link ArtifactRepository}
     * @param pluginRepositories {@link ArtifactRepository}
     * @param buildingRequest {@link ProjectBuildingRequest}
     * @param repositoryMetadataManager {@link RepositoryMetadataManager}
     * @param wagonManager {@link WagonManager}
     */
    public RepositoryUtils( Log log, WagonRepositoryConnectorFactory wagonRepositoryConnectorFactory,
                            ProjectBuilder projectBuilder, RepositorySystem repositorySystem, ArtifactResolver resolver,
                            List<ArtifactRepository> remoteRepositories, List<ArtifactRepository> pluginRepositories,
                            ProjectBuildingRequest buildingRequest,
                            RepositoryMetadataManager repositoryMetadataManager )
    {
        this.log = log;
        this.wagonRepositoryConnectorFactory = wagonRepositoryConnectorFactory;
        this.projectBuilder = projectBuilder;
        this.repositorySystem = repositorySystem;
        this.resolver = resolver;
        this.remoteRepositories = remoteRepositories;
        this.pluginRepositories = pluginRepositories;
        this.buildingRequest = buildingRequest;
    }

    /**
     * @param artifact not null
     * @throws ArtifactResolverException if any 
     */
    public void resolve( Artifact artifact )
        throws ArtifactResolverException
    {
        List<ArtifactRepository> repos =
            new ArrayList<ArtifactRepository>( pluginRepositories.size() + remoteRepositories.size() );
        repos.addAll( pluginRepositories );
        repos.addAll( remoteRepositories );
        
        ProjectBuildingRequest buildRequest = new DefaultProjectBuildingRequest( buildingRequest );
        buildRequest.setRemoteRepositories( repos );

        ArtifactResult result = resolver.resolveArtifact( buildRequest , artifact );
        artifact.setFile( result.getArtifact().getFile() );
    }

    /**
     * @param repo not null
     * @param artifact not null
     * @return <code>true</code> if the artifact exists in the given repo, <code>false</code> otherwise or if
     * the repo is blacklisted.
     */
    public boolean dependencyExistsInRepo( ArtifactRepository repo, Artifact artifact )
    {
        if ( repo.isBlacklisted() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "The repo '" + repo.getId() + "' is black listed - Ignored it" );
            }
            return false;
        }

        if ( UNKNOWN_HOSTS.contains( repo.getUrl() ) )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "The repo url '" + repo.getUrl() + "' is unknown - Ignored it" );
            }
            return false;
        }

        WagonRepositoryConnector wagonRepositoryConnector;
        try
        {
            wagonRepositoryConnector = wagonRepositoryConnectorFactory.newInstance( buildingRequest, repo );
        }
        catch ( WagonRepositoryConnectorException e )
        {
            logError( "Unsupported protocol: '" + repo.getProtocol() + "'", e );
            return false;
        }

        try
        {
            String resource =
                StringUtils.replace( getDependencyUrlFromRepository( artifact, repo ), repo.getUrl(), "" );

            return wagonRepositoryConnector.resourceExists( resource );
        }
        catch ( TransferFailedException e )
        {
            if ( e.getCause() instanceof UnknownHostException )
            {
                log.error( "Unknown host " + e.getCause().getMessage() + " - ignored it" );
                UNKNOWN_HOSTS.add( repo.getUrl() );
            }
            else
            {
                logError( "Unable to determine if resource " + artifact + " exists in " + repo.getUrl(), e );
            }
            return false;
        }
        catch ( AuthorizationException e )
        {
            logError( "Unable to connect to: " + repo.getUrl(), e );
            return false;
        }
        finally
        {
            wagonRepositoryConnector.close();
        }
    }

    /**
     * Get the <code>Maven project</code> from the repository depending the <code>Artifact</code> given.
     *
     * @param artifact an artifact
     * @return the Maven project for the given artifact
     * @throws ProjectBuildingException if any
     */
    public MavenProject getMavenProjectFromRepository( Artifact artifact )
        throws ProjectBuildingException
    {
        Artifact projectArtifact = artifact;

        boolean allowStubModel = false;
        if ( !"pom".equals( artifact.getType() ) )
        {
            projectArtifact = repositorySystem.createProjectArtifact( artifact.getGroupId(), 
                                                                      artifact.getArtifactId(),
                                                                      artifact.getVersion() );
            allowStubModel = true;
        }

        return projectBuilder.build( projectArtifact, allowStubModel, buildingRequest ).getProject();
    }

    /**
     * @param artifact not null
     * @param repo not null
     * @return the artifact url in the given repo for the given artifact. If it is a snapshot artifact, the version
     * will be the timestamp and the build number from the metadata. Could return null if the repo is blacklisted.
     */
    public String getDependencyUrlFromRepository( Artifact artifact, ArtifactRepository repo )
    {
        if ( repo.isBlacklisted() )
        {
            return null;
        }

        Artifact copyArtifact = ArtifactUtils.copyArtifact( artifact );
        // Try to get the last artifact repo name depending the snapshot version
        if ( ( artifact.isSnapshot() && repo.getSnapshots().isEnabled() ) )
        {
            if ( artifact.getBaseVersion().equals( artifact.getVersion() ) )
            {
                // Try to resolve it if not already done
                if ( artifact.getMetadataList() == null || artifact.getMetadataList().isEmpty() )
                {
                    try
                    {
                        resolve( artifact );
                    }
                    catch ( ArtifactResolverException e )
                    {
                        log.error( "Artifact: " + artifact.getId() + " could not be resolved." );
                    }
                }
            }
        }

        return repo.getUrl() + "/" + repo.pathOf( copyArtifact );
    }

    /**
     * Log an error, adding the stacktrace only is debug is enabled.
     * 
     * @param message the error message
     * @param e the cause
     */
    private void logError( String message, Exception e )
    {
        if ( log.isDebugEnabled() )
        {
            log.error( message, e );
        }
        else
        {
            log.error( message );
        }
    }
}
