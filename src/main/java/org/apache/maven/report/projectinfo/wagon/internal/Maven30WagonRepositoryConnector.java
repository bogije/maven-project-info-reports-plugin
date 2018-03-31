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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.maven.report.projectinfo.wagon.WagonRepositoryConnector;
import org.apache.maven.report.projectinfo.wagon.WagonRepositoryConnectorException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.Debug;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.spi.connector.RepositoryConnector;

/**
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
class Maven30WagonRepositoryConnector implements WagonRepositoryConnector, LogEnabled
{
    private Wagon wagon;
    
    Maven30WagonRepositoryConnector( RepositoryConnector connector )
        throws WagonRepositoryConnectorException
    {
        Method pollWagonMethod;
        try
        {
            pollWagonMethod = connector.getClass().getDeclaredMethod( "pollWagon" );
            pollWagonMethod.setAccessible( true );
            wagon = (Wagon) pollWagonMethod.invoke( connector );
            
            wagon.setTimeout( 1000 );
        }
        catch ( NoSuchMethodException e )
        {
            throw new WagonRepositoryConnectorException( e.getMessage(), e );
        }
        catch ( SecurityException e )
        {
            throw new WagonRepositoryConnectorException( e.getMessage(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new WagonRepositoryConnectorException( e.getMessage(), e );
        }
        catch ( IllegalArgumentException e )
        {
            throw new WagonRepositoryConnectorException( e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new WagonRepositoryConnectorException( e.getMessage(), e );
        }
    }

    @Override
    public boolean resourceExists( String resourceName ) throws TransferFailedException, AuthorizationException
    {
        return wagon.resourceExists( resourceName );
    }
    
    @Override
    public void close()
    {
        try
        {
            if ( wagon != null )
            {
                wagon.disconnect();
            }
        }
        catch ( Exception e )
        {
            // too bad
        }
    }
    
    @Override
    public void enableLogging( Logger logger )
    {
        if ( logger.isDebugEnabled() )
        {
            Debug debug = new Debug();

            wagon.addSessionListener( debug );
            wagon.addTransferListener( debug );
        }
    }
    
}
