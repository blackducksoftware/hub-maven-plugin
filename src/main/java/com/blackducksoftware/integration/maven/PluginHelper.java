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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.build.Constants;
import com.blackducksoftware.integration.build.DependencyNode;
import com.blackducksoftware.integration.build.utils.BdioDependencyWriter;
import com.blackducksoftware.integration.build.utils.FlatDependencyListWriter;
import com.blackducksoftware.integration.hub.api.bom.BomImportRestService;
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusItem;
import com.blackducksoftware.integration.hub.dataservices.DataServicesFactory;
import com.blackducksoftware.integration.hub.dataservices.policystatus.PolicyStatusDataService;
import com.blackducksoftware.integration.hub.dataservices.scan.ScanStatusDataService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.MissingUUIDException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.ResourceDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

public class PluginHelper {
	private final Logger logger = LoggerFactory.getLogger(PluginHelper.class);

	public void createHubOutput(final MavenProject project, final MavenSession session,
			final DependencyGraphBuilder dependencyGraphBuilder, final File outputDirectory, final String filename,
			final String hubProjectName, final String hubProjectVersion) throws MojoExecutionException, IOException {
		final MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor(dependencyGraphBuilder,
				session);
		final DependencyNode rootNode = mavenDependencyExtractor.getRootDependencyNode(project, hubProjectName,
				hubProjectVersion);

		final BdioDependencyWriter bdioDependencyWriter = new BdioDependencyWriter();
		bdioDependencyWriter.write(outputDirectory, filename, hubProjectName, rootNode);
	}

	public void createFlatOutput(final MavenProject project, final MavenSession session,
			final DependencyGraphBuilder dependencyGraphBuilder, final File outputDirectory, final String filename,
			final String hubProjectName, final String hubProjectVersion) throws MojoExecutionException, IOException {
		final MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor(dependencyGraphBuilder,
				session);
		final DependencyNode rootNode = mavenDependencyExtractor.getRootDependencyNode(project, hubProjectName,
				hubProjectVersion);

		final FlatDependencyListWriter flatDependencyListWriter = new FlatDependencyListWriter();
		flatDependencyListWriter.write(outputDirectory, filename, rootNode);
	}

	public void deployHubOutput(final Slf4jIntLogger logger, final RestConnection restConnection,
			final File outputDirectory, final String filename)
			throws IOException, ResourceDoesNotExistException, URISyntaxException, BDRestException {
		final DataServicesFactory dataServicesFactory = new DataServicesFactory(restConnection);
		final BomImportRestService bomImportRestService = dataServicesFactory.getBomImportRestService();

		final File file = new File(outputDirectory, filename);
		bomImportRestService.importBomFile(file, Constants.BDIO_FILE_MEDIA_TYPE);

		logger.info(String.format("Uploaded the file %s to %s", file, restConnection.getBaseUrl()));
	}

	public void waitForHub(final RestConnection restConnection, final String hubProjectName,
			final String hubProjectVersion, final long scanStartedTimeout, final long scanFinishedTimeout) {
		try {
			final DataServicesFactory dataServicesFactory = new DataServicesFactory(restConnection);
			final ScanStatusDataService scanStatusDataService = dataServicesFactory.createScanStatusDataService();
			scanStatusDataService.assertBomImportScanStartedThenFinished(hubProjectName, hubProjectVersion,
					scanStartedTimeout * 1000, scanFinishedTimeout * 1000, new Slf4jIntLogger(logger));
		} catch (IOException | BDRestException | URISyntaxException | ProjectDoesNotExistException
				| UnexpectedHubResponseException | HubIntegrationException | InterruptedException e) {
			logger.error(String.format("There was an error waiting for the scans: %s", e.getMessage()), e);
		}
	}

	public PolicyStatusItem checkPolicies(final RestConnection restConnection, final String hubProjectName,
			final String hubProjectVersion) throws MojoFailureException, IOException, URISyntaxException,
			BDRestException, ProjectDoesNotExistException, HubIntegrationException, MissingUUIDException {
		final DataServicesFactory dataServicesFactory = new DataServicesFactory(restConnection);
		final PolicyStatusDataService policyStatusDataService = dataServicesFactory.createPolicyStatusDataService();

		final PolicyStatusItem policyStatusItem = policyStatusDataService
				.getPolicyStatusForProjectAndVersion(hubProjectName, hubProjectVersion);
		return policyStatusItem;
	}

}
