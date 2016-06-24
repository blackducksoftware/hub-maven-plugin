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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.restlet.data.MediaType;
import org.restlet.representation.FileRepresentation;

import com.blackducksoftware.integration.build.bdio.Constants;
import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.builder.ValidationResultEnum;
import com.blackducksoftware.integration.hub.builder.ValidationResults;
import com.blackducksoftware.integration.hub.capabilities.HubCapabilitiesEnum;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.EncryptionException;
import com.blackducksoftware.integration.hub.exception.ResourceDoesNotExistException;
import com.blackducksoftware.integration.hub.global.GlobalFieldKey;
import com.blackducksoftware.integration.hub.global.HubProxyInfo;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.maven.logging.MavenLogger;

@Mojo(name = "deployHubOutput", defaultPhase = LifecyclePhase.PACKAGE)
public class BuildInfoHubDeployment extends AbstractMojo {
	@Parameter(defaultValue = PluginConstants.PARAM_PROJECT, readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = PluginConstants.PARAM_SESSION, readonly = true, required = true)
	private MavenSession session;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File target;

	@Parameter(defaultValue = PluginConstants.PARAM_HUB_URL, readonly = true)
	private String hubUrl;

	@Parameter(defaultValue = PluginConstants.PARAM_HUB_USER, readonly = true)
	private String hubUser;

	@Parameter(defaultValue = PluginConstants.PARAM_HUB_PASSWORD, readonly = true)
	private String hubPassword;

	@Parameter(defaultValue = PluginConstants.PARAM_HUB_TIMEOUT, readonly = true)
	private String hubTimeout;

	@Parameter(defaultValue = PluginConstants.PARAM_HUB_PROXY_HOST, readonly = true)
	private String hubProxyHost;

	@Parameter(defaultValue = PluginConstants.PARAM_HUB_PROXY_PORT, readonly = true)
	private String hubProxyPort;

	@Parameter(defaultValue = PluginConstants.PARAM_HUB_PROXY_NO_HOSTS, readonly = true)
	private String hubNoProxyHosts;

	@Parameter(defaultValue = PluginConstants.PARAM_HUB_PROXY_USER, readonly = true)
	private String hubProxyUser;

	@Parameter(defaultValue = PluginConstants.PARAM_HUB_PROXY_PASSWORD, readonly = true)
	private String hubProxyPassword;

	private final MavenLogger logger = new MavenLogger(getLog());
	private final PluginHelper helper = new PluginHelper();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final String pluginTaskString = "BlackDuck Software " + project.getName() + Constants.BDIO_FILE_SUFFIX
				+ " file deployment";
		getLog().info(pluginTaskString + " starting...");

		final HubServerConfigBuilder builder = new HubServerConfigBuilder();
		builder.setHubUrl(hubUrl);
		builder.setUsername(hubUser);
		builder.setPassword(hubPassword);
		builder.setTimeout(hubTimeout);
		builder.setProxyHost(hubProxyHost);
		builder.setProxyPort(hubProxyPort);
		builder.setIgnoredProxyHosts(hubNoProxyHosts);
		builder.setProxyUsername(hubProxyUser);
		builder.setProxyPassword(hubProxyPassword);

		final ValidationResults<GlobalFieldKey, HubServerConfig> results = builder.build();

		if (results.isSuccess()) {
			try {
				final HubServerConfig config = results.getConstructedObject();
				uploadFileToHub(config);
			} catch (final URISyntaxException e) {
				throw new MojoExecutionException("Hub URI invalid", e);
			} catch (final IllegalArgumentException | BDRestException | EncryptionException | IOException e) {
				throw new MojoExecutionException("Cannot communicate with hub server.", e);
			} catch (final ResourceDoesNotExistException e) {
				throw new MojoExecutionException("Cannot upload the file to the hub server.", e);
			}
		} else {
			logErrors(results);
		}
		logger.info(pluginTaskString + " finished...");
	}

	private void uploadFileToHub(final HubServerConfig config) throws URISyntaxException, IllegalArgumentException,
			BDRestException, EncryptionException, IOException, ResourceDoesNotExistException {
		final RestConnection connection = new RestConnection(config.getHubUrl().toString());
		final HubProxyInfo proxyInfo = config.getProxyInfo();
		if (proxyInfo.shouldUseProxyForUrl(config.getHubUrl())) {
			connection.setProxyProperties(proxyInfo);
		}
		connection.setCookies(config.getGlobalCredentials().getUsername(),
				config.getGlobalCredentials().getDecryptedPassword());

		if (isUploadSupported(connection)) {
			final List<String> urlSegments = new ArrayList<>();
			urlSegments.add("api");
			urlSegments.add("v1");
			urlSegments.add("bom-import");
			final Set<SimpleEntry<String, String>> queryParameters = new HashSet<>();
			final File file = new File(target, helper.getBDIOFileName(project));
			final FileRepresentation content = new FileRepresentation(file, MediaType.APPLICATION_JSON);
			final String location = connection.httpPostFromRelativeUrl(urlSegments, queryParameters, content);

			logger.info("Uploaded the file: " + file + " to " + location);
		} else {
			logger.error("The hub server does not support file deployment of " + helper.getBDIOFileName(project));
		}
	}

	private void logErrors(final ValidationResults<GlobalFieldKey, HubServerConfig> results) {
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

	private boolean isUploadSupported(final RestConnection restConnection) throws URISyntaxException, IOException {
		final HubIntRestService service = new HubIntRestService(restConnection);
		final HubSupportHelper hubSupport = new HubSupportHelper();
		hubSupport.checkHubSupport(service, logger);
		return hubSupport.hasCapability(HubCapabilitiesEnum.BOM_FILE_UPLOAD);
	}

}
