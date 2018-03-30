package org.apache.maven.report.projectinfo.wagon;

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

import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;

/**
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
public interface WagonRepositoryConnector
{
    boolean resourceExists( String resourceName ) throws TransferFailedException, AuthorizationException;
    
    /**
     * Closes this connector and frees any network resources associated with it. Once closed, a connector must not be
     * used for further transfers. Closing an already closed connector has no effect.
     */
    void close();
}
