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

import static com.blackducksoftware.integration.build.Constants.CREATE_HUB_OUTPUT_ERROR;
import static com.blackducksoftware.integration.build.Constants.DEPLOY_HUB_OUTPUT;
import static com.blackducksoftware.integration.build.Constants.DEPLOY_HUB_OUTPUT_ERROR;
import static com.blackducksoftware.integration.build.Constants.DEPLOY_HUB_OUTPUT_FINISHED;
import static com.blackducksoftware.integration.build.Constants.DEPLOY_HUB_OUTPUT_STARTING;
import static com.blackducksoftware.integration.build.Constants.FAILED_TO_CREATE_REPORT;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.api.HubServicesFactory;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.ResourceDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection;
import com.blackducksoftware.integration.hub.rest.RestConnection;

@Mojo(name = DEPLOY_HUB_OUTPUT, requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PACKAGE, aggregator = true)
public class DeployHubOutputGoal extends HubMojo {
    private final Logger logger = LoggerFactory.getLogger(DeployHubOutputGoal.class);

    private long hubScanStartedTimeout = 300;

    private long hubScanFinishedTimeout = 300;

    @Override
    public void performGoal() throws MojoExecutionException, MojoFailureException {
        logger.info(String.format(DEPLOY_HUB_OUTPUT_STARTING, getBdioFilename()));

        try {
            PLUGIN_HELPER.createHubOutput(getProject(), getSession(), getDependencyGraphBuilder(), getOutputDirectory(),
                    getHubProjectName(), getHubVersionName());
        } catch (final IOException e) {
            throw new MojoFailureException(String.format(CREATE_HUB_OUTPUT_ERROR, e.getMessage()), e);
        }

        final HubServerConfig hubServerConfig = getHubServerConfigBuilder().build();
        RestConnection restConnection;
        try {
            restConnection = new CredentialsRestConnection(hubServerConfig);
            HubServicesFactory services = new HubServicesFactory(restConnection);
            PLUGIN_HELPER.deployHubOutput(services, getOutputDirectory(),
                    getHubProjectName());
            if (getCreateHubReport()) {
                PLUGIN_HELPER.waitForHub(services, getHubProjectName(), getHubVersionName(), getHubScanStartedTimeout(),
                        getHubScanFinishedTimeout());
                File reportOutput = new File(getOutputDirectory(), "report");
                try {
                    PLUGIN_HELPER.createRiskReport(services, reportOutput, getHubProjectName(), getHubVersionName());
                } catch (IllegalArgumentException | URISyntaxException | BDRestException | IOException
                        | ProjectDoesNotExistException | HubIntegrationException | InterruptedException | UnexpectedHubResponseException e) {
                    throw new MojoFailureException(String.format(FAILED_TO_CREATE_REPORT, e.getMessage()), e);
                }
            }
        } catch (IllegalArgumentException | URISyntaxException | BDRestException | EncryptionException | IOException
                | ResourceDoesNotExistException e) {
            throw new MojoFailureException(String.format(DEPLOY_HUB_OUTPUT_ERROR, e.getMessage()), e);
        }

        logger.info(String.format(DEPLOY_HUB_OUTPUT_FINISHED, getBdioFilename()));
    }

    public long getHubScanStartedTimeout() {
        return hubScanStartedTimeout;
    }

    public void setHubScanStartedTimeout(long hubScanStartedTimeout) {
        this.hubScanStartedTimeout = hubScanStartedTimeout;
    }

    public long getHubScanFinishedTimeout() {
        return hubScanFinishedTimeout;
    }

    public void setHubScanFinishedTimeout(long hubScanFinishedTimeout) {
        this.hubScanFinishedTimeout = hubScanFinishedTimeout;
    }

}
