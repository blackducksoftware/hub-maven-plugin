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

import static com.blackducksoftware.integration.maven.PluginConstants.EXCEPTION_MSG_FILE_NOT_CREATED;
import static com.blackducksoftware.integration.maven.PluginConstants.MSG_FILE_TO_GENERATE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.restlet.data.MediaType;
import org.restlet.representation.FileRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.build.bdio.BdioConverter;
import com.blackducksoftware.integration.build.bdio.CommonBomFormatter;
import com.blackducksoftware.integration.build.bdio.Constants;
import com.blackducksoftware.integration.build.bdio.DependencyNode;
import com.blackducksoftware.integration.builder.ValidationResultEnum;
import com.blackducksoftware.integration.builder.ValidationResults;
import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.ResourceDoesNotExistException;
import com.blackducksoftware.integration.hub.global.GlobalFieldKey;
import com.blackducksoftware.integration.hub.global.HubProxyInfo;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

public class PluginHelper {
	private final Logger logger = LoggerFactory.getLogger(PluginHelper.class);

	public String getBDIOFileName(final MavenProject project) {
		return project.getArtifactId() + Constants.BDIO_FILE_SUFFIX;
	}

	public void createHubOutput(final MavenProject project, final MavenSession session, final File target,
			final DependencyGraphBuilder dependencyGraphBuilder) throws MojoExecutionException {
		logger.info("projects...");
		final MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor(dependencyGraphBuilder,
				session);
		final String pluginTaskString = "BlackDuck Software " + getBDIOFileName(project) + " file generation";
		logger.info(pluginTaskString + " starting...");
		final DependencyNode rootNode = mavenDependencyExtractor.getRootDependencyNode(project);
		for (final MavenProject moduleProject : project.getCollectedProjects()) {
			final DependencyNode moduleRootNode = mavenDependencyExtractor.getRootDependencyNode(moduleProject);
			rootNode.getChildren().addAll(moduleRootNode.getChildren());
		}

		createBDIOFile(project, target, rootNode);
		logger.info("..." + pluginTaskString + " finished");
	}

	private void createBDIOFile(final MavenProject project, final File target, final DependencyNode rootDependencyNode)
			throws MojoExecutionException {
		try {
			// if the directory doesn't exist yet, let's create it
			target.mkdirs();

			final File file = new File(target, getBDIOFileName(project));
			logger.info(MSG_FILE_TO_GENERATE + file.getCanonicalPath());

			try (final OutputStream outputStream = new FileOutputStream(file)) {
				final BdioConverter bdioConverter = new BdioConverter();
				final CommonBomFormatter commonBomFormatter = new CommonBomFormatter(bdioConverter);
				commonBomFormatter.writeProject(outputStream, project.getName(), rootDependencyNode);
			}
		} catch (final IOException e) {
			throw new MojoExecutionException(EXCEPTION_MSG_FILE_NOT_CREATED, e);
		}
	}

	public void uploadFileToHub(final Slf4jIntLogger logger, final HubServerConfigBuilder hubServerConfigBuilder,
			final MavenProject project, final File target) throws MojoExecutionException, MojoFailureException {
		final ValidationResults<GlobalFieldKey, HubServerConfig> results = hubServerConfigBuilder.buildResults();

		if (results.isSuccess()) {
			try {
				final HubServerConfig hubServerConfig = results.getConstructedObject();
				uploadFileToHub(logger, hubServerConfig, project, target);
			} catch (final URISyntaxException e) {
				throw new MojoExecutionException("Hub URI invalid", e);
			} catch (final IllegalArgumentException | BDRestException | EncryptionException | IOException e) {
				throw new MojoExecutionException("Cannot communicate with hub server.", e);
			} catch (final ResourceDoesNotExistException e) {
				throw new MojoExecutionException("Cannot upload the file to the hub server.", e);
			}
		} else {
			logErrors(logger, results);
		}
	}

	private void uploadFileToHub(final Slf4jIntLogger logger, final HubServerConfig config, final MavenProject project,
			final File target) throws URISyntaxException, IllegalArgumentException, BDRestException,
			EncryptionException, IOException, ResourceDoesNotExistException {
		final RestConnection connection = new RestConnection(config.getHubUrl().toString());
		final HubProxyInfo proxyInfo = config.getProxyInfo();
		if (proxyInfo.shouldUseProxyForUrl(config.getHubUrl())) {
			connection.setProxyProperties(proxyInfo);
		}
		connection.setCookies(config.getGlobalCredentials().getUsername(),
				config.getGlobalCredentials().getDecryptedPassword());

		final List<String> urlSegments = new ArrayList<>();
		urlSegments.add("api");
		urlSegments.add("v1");
		urlSegments.add("bom-import");
		final Set<SimpleEntry<String, String>> queryParameters = new HashSet<>();
		final File file = new File(target, getBDIOFileName(project));
		final FileRepresentation content = new FileRepresentation(file, new MediaType(Constants.BDIO_FILE_MEDIA_TYPE));
		final String location = connection.httpPostFromRelativeUrl(urlSegments, queryParameters, content);

		logger.info("Uploaded the file: " + file + " to " + config.getHubUrl().toString());
	}

	private void logErrors(final Slf4jIntLogger logger,
			final ValidationResults<GlobalFieldKey, HubServerConfig> results) {
		logger.error("Invalid Hub Server Configuration skipping file deployment");
		logger.error("Caused by: ");

		final Set<GlobalFieldKey> keySet = results.getResultMap().keySet();

		for (final GlobalFieldKey key : keySet) {
			if (results.hasWarnings(key)) {
				logger.warn(results.getResultString(key, ValidationResultEnum.WARN));
			}
			if (results.hasErrors(key)) {
				logger.error(results.getResultString(key, ValidationResultEnum.ERROR));
			}
		}
	}

}
