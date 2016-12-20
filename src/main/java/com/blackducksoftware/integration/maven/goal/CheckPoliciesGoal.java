/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.integration.maven.goal;

import static com.blackducksoftware.integration.build.Constants.CHECK_POLICIES;
import static com.blackducksoftware.integration.build.Constants.CHECK_POLICIES_ERROR;
import static com.blackducksoftware.integration.build.Constants.CHECK_POLICIES_FINISHED;
import static com.blackducksoftware.integration.build.Constants.CHECK_POLICIES_STARTING;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusItem;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;

@Mojo(name = CHECK_POLICIES, defaultPhase = LifecyclePhase.PACKAGE)
public class CheckPoliciesGoal extends HubMojo {
    @Override
    public void performGoal() throws MojoExecutionException, MojoFailureException {
        logger.info(String.format(CHECK_POLICIES_STARTING, getBdioFilename()));

        final HubServerConfig hubServerConfig = getHubServerConfigBuilder().build();
        try {
            final RestConnection restConnection = new CredentialsRestConnection(hubServerConfig);
            HubServicesFactory services = new HubServicesFactory(restConnection);
            final PolicyStatusItem policyStatusItem = PLUGIN_HELPER.checkPolicies(services, getHubProjectName(),
                    getHubVersionName());
            handlePolicyStatusItem(policyStatusItem);
        } catch (IllegalArgumentException | EncryptionException | HubIntegrationException e) {
            throw new MojoFailureException(String.format(CHECK_POLICIES_ERROR, e.getMessage()), e);
        }

        logger.info(String.format(CHECK_POLICIES_FINISHED, getBdioFilename()));
    }

}
