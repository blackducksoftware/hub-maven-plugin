/**
 * hub-maven-plugin
 *
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
 */
package com.blackducksoftware.integration.maven;

import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.BDIO_FILE_MEDIA_TYPE;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.SCAN_ERROR_MESSAGE;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.UPLOAD_FILE_MESSAGE;

import java.io.File;
import java.io.IOException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.api.bom.BomImportRequestService;
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusItem;
import com.blackducksoftware.integration.hub.buildtool.DependencyNode;
import com.blackducksoftware.integration.hub.buildtool.FlatDependencyListWriter;
import com.blackducksoftware.integration.hub.buildtool.HubProjectDetailsWriter;
import com.blackducksoftware.integration.hub.buildtool.bdio.BdioDependencyWriter;
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDataService;
import com.blackducksoftware.integration.hub.dataservice.report.RiskReportDataService;
import com.blackducksoftware.integration.hub.dataservice.scan.ScanStatusDataService;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

public class PluginHelper {
    private final Logger logger = LoggerFactory.getLogger(PluginHelper.class);

    public void createFlatOutput(final MavenProject project, final MavenSession session,
            final DependencyGraphBuilder dependencyGraphBuilder, final File outputDirectory,
            final String hubProjectName, final String hubProjectVersion, final String excludedModules, final String includedScopes)
            throws MojoExecutionException, IOException {
        final MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor(excludedModules, includedScopes);
        final DependencyNode rootNode = mavenDependencyExtractor.getRootDependencyNode(dependencyGraphBuilder, session, project, hubProjectName,
                hubProjectVersion);

        final FlatDependencyListWriter flatDependencyListWriter = new FlatDependencyListWriter();
        flatDependencyListWriter.write(outputDirectory, hubProjectName, rootNode);

        final HubProjectDetailsWriter hubProjectDetailsWriter = new HubProjectDetailsWriter();
        hubProjectDetailsWriter.write(outputDirectory, hubProjectName, hubProjectVersion);
    }

    public void createHubOutput(final MavenProject project, final MavenSession session,
            final DependencyGraphBuilder dependencyGraphBuilder, final File outputDirectory,
            final String hubProjectName, final String hubProjectVersion, final String excludedModules, final String includedScopes)
            throws MojoExecutionException, IOException {
        final MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor(excludedModules, includedScopes);
        final DependencyNode rootNode = mavenDependencyExtractor.getRootDependencyNode(dependencyGraphBuilder, session, project, hubProjectName,
                hubProjectVersion);

        final BdioDependencyWriter bdioDependencyWriter = new BdioDependencyWriter();
        bdioDependencyWriter.write(outputDirectory, project.getArtifactId(), hubProjectName, rootNode);

        final HubProjectDetailsWriter hubProjectDetailsWriter = new HubProjectDetailsWriter();
        hubProjectDetailsWriter.write(outputDirectory, hubProjectName, hubProjectVersion);
    }

    public void deployHubOutput(final HubServicesFactory services,
            final File outputDirectory, final String hubProjectName) throws HubIntegrationException {
        final String filename = BdioDependencyWriter.getFilename(hubProjectName);
        final File file = new File(outputDirectory, filename);
        final BomImportRequestService bomImportRequestService = services.createBomImportRequestService();
        bomImportRequestService.importBomFile(file, BDIO_FILE_MEDIA_TYPE);

        logger.info(String.format(UPLOAD_FILE_MESSAGE, file, bomImportRequestService.getRestConnection().getBaseUrl()));
    }

    public void waitForHub(final HubServicesFactory services, final String hubProjectName,
            final String hubProjectVersion, final long scanStartedTimeout, final long scanFinishedTimeout) {
        final ScanStatusDataService scanStatusDataService = services.createScanStatusDataService(new Slf4jIntLogger(logger));
        try {
            scanStatusDataService.assertBomImportScanStartedThenFinished(hubProjectName, hubProjectVersion,
                    scanStartedTimeout * 1000, scanFinishedTimeout * 1000, new Slf4jIntLogger(logger));
        } catch (final HubIntegrationException e) {
            logger.error(String.format(SCAN_ERROR_MESSAGE, e.getMessage()), e);
        }
    }

    public void createRiskReport(final HubServicesFactory services,
            final File outputDirectory, final String projectName, final String projectVersionName)
            throws HubIntegrationException {
        final RiskReportDataService reportDataService = services.createRiskReportDataService(new Slf4jIntLogger(logger));
        reportDataService.createRiskReportFiles(outputDirectory, projectName, projectVersionName);
    }

    public PolicyStatusItem checkPolicies(final HubServicesFactory services, final String hubProjectName,
            final String hubProjectVersion) throws HubIntegrationException {
        final PolicyStatusDataService policyStatusDataService = services.createPolicyStatusDataService(new Slf4jIntLogger(logger));
        final PolicyStatusItem policyStatusItem = policyStatusDataService
                .getPolicyStatusForProjectAndVersion(hubProjectName, hubProjectVersion);
        return policyStatusItem;
    }

}
