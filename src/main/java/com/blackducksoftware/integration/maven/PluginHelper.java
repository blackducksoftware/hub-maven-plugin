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
package com.blackducksoftware.integration.maven;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.build.Constants;
import com.blackducksoftware.integration.build.DependencyNode;
import com.blackducksoftware.integration.build.utils.BdioDependencyWriter;
import com.blackducksoftware.integration.build.utils.FlatDependencyListWriter;
import com.blackducksoftware.integration.hub.api.HubServicesFactory;
import com.blackducksoftware.integration.hub.api.bom.BomImportRestService;
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusItem;
import com.blackducksoftware.integration.hub.dataservices.policystatus.PolicyStatusDataService;
import com.blackducksoftware.integration.hub.dataservices.report.RiskReportDataService;
import com.blackducksoftware.integration.hub.dataservices.scan.ScanStatusDataService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.MissingUUIDException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.ResourceDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

public class PluginHelper {
    private final Logger logger = LoggerFactory.getLogger(PluginHelper.class);

    public void createFlatOutput(final MavenProject project, final MavenSession session,
            final DependencyGraphBuilder dependencyGraphBuilder, final File outputDirectory,
            final String hubProjectName, final String hubProjectVersion) throws MojoExecutionException, IOException {
        final MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor();
        final DependencyNode rootNode = mavenDependencyExtractor.getRootDependencyNode(dependencyGraphBuilder, session, project, hubProjectName,
                hubProjectVersion);

        final FlatDependencyListWriter flatDependencyListWriter = new FlatDependencyListWriter();
        flatDependencyListWriter.write(outputDirectory, hubProjectName, rootNode);
    }

    public void createHubOutput(final MavenProject project, final MavenSession session,
            final DependencyGraphBuilder dependencyGraphBuilder, final File outputDirectory,
            final String hubProjectName, final String hubProjectVersion) throws MojoExecutionException, IOException {
        final MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor();
        final DependencyNode rootNode = mavenDependencyExtractor.getRootDependencyNode(dependencyGraphBuilder, session, project, hubProjectName,
                hubProjectVersion);

        final BdioDependencyWriter bdioDependencyWriter = new BdioDependencyWriter();
        bdioDependencyWriter.write(outputDirectory, hubProjectName, rootNode);
    }

    public void deployHubOutput(final HubServicesFactory services,
            final File outputDirectory, final String hubProjectName) throws IOException, ResourceDoesNotExistException, URISyntaxException, BDRestException {
        String filename = BdioDependencyWriter.getFilename(hubProjectName);
        final File file = new File(outputDirectory, filename);
        final BomImportRestService bomImportRestService = services.createBomImportRestService();
        bomImportRestService.importBomFile(file, Constants.BDIO_FILE_MEDIA_TYPE);

        logger.info(String.format(Constants.UPLOAD_FILE_MESSAGE, file, bomImportRestService.getRestConnection().getBaseUrl()));
    }

    public void waitForHub(final HubServicesFactory services, final String hubProjectName,
            final String hubProjectVersion, final long scanStartedTimeout, final long scanFinishedTimeout) {
        final ScanStatusDataService scanStatusDataService = services.createScanStatusDataService();
        try {
            scanStatusDataService.assertBomImportScanStartedThenFinished(hubProjectName, hubProjectVersion,
                    scanStartedTimeout * 1000, scanFinishedTimeout * 1000, new Slf4jIntLogger(logger));
        } catch (IOException | BDRestException | URISyntaxException | ProjectDoesNotExistException | UnexpectedHubResponseException
                | HubIntegrationException | InterruptedException e) {
            logger.error(String.format(Constants.SCAN_ERROR_MESSAGE, e.getMessage()), e);
        }
    }

    public void createRiskReport(final HubServicesFactory services,
            final File outputDirectory, String projectName, String projectVersionName)
            throws IOException, BDRestException, URISyntaxException, HubIntegrationException, InterruptedException, UnexpectedHubResponseException,
            ProjectDoesNotExistException {
        RiskReportDataService reportDataService = services.createRiskReportDataService(new Slf4jIntLogger(logger));
        reportDataService.createRiskReport(outputDirectory, projectName, projectVersionName);
    }

    public PolicyStatusItem checkPolicies(final HubServicesFactory services, final String hubProjectName,
            final String hubProjectVersion) throws IOException, URISyntaxException, BDRestException, ProjectDoesNotExistException, HubIntegrationException,
            MissingUUIDException, UnexpectedHubResponseException {
        final PolicyStatusDataService policyStatusDataService = services.createPolicyStatusDataService();
        final PolicyStatusItem policyStatusItem = policyStatusDataService
                .getPolicyStatusForProjectAndVersion(hubProjectName, hubProjectVersion);
        return policyStatusItem;
    }

}
