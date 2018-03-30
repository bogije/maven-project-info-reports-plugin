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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.report.projectinfo.wagon.WagonRepositoryConnector;
import org.apache.maven.report.projectinfo.wagon.WagonRepositoryConnectorException;
import org.apache.maven.report.projectinfo.wagon.WagonRepositoryConnectorFactory;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
@Component( role = WagonRepositoryConnectorFactory.class )
public class DefaultWagonRepositoryConnectorFactory implements WagonRepositoryConnectorFactory, Contextualizable
{
    private Logger logger;
    
    private PlexusContainer container;

    @Override
    public WagonRepositoryConnector newInstance( ProjectBuildingRequest request, ArtifactRepository repository )
        throws WagonRepositoryConnectorException
    {
        String hint = isMaven31() ? "maven31" : "maven3";
        try
        {
            WagonRepositoryConnectorFactory factory = container.lookup( WagonRepositoryConnectorFactory.class, hint );
            
            factory.enableLogging( logger );
            
            return factory.newInstance( request, repository );
        }
        catch ( ComponentLookupException e )
        {
            throw new WagonRepositoryConnectorException( e.getMessage(), e );
        }
    }

    protected static boolean isMaven31()
    {
        return canFindCoreClass( "org.eclipse.aether.artifact.Artifact" ); // Maven 3.1 specific
    }

    private static boolean canFindCoreClass( String className )
    {
        try
        {
            Thread.currentThread().getContextClassLoader().loadClass( className );

            return true;
        }
        catch ( ClassNotFoundException e )
        {
            return false;
        }
    }
    
    /**
     * Injects the Plexus content.
     *
     * @param context Plexus context to inject.
     * @throws ContextException if the PlexusContainer could not be located.
     */
    @Override
    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    @Override
    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }
}
