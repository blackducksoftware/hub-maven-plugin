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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;

import com.blackducksoftware.integration.build.bdio.BdioConverter;
import com.blackducksoftware.integration.build.bdio.CommonBomFormatter;
import com.blackducksoftware.integration.build.bdio.DependencyNode;
import com.blackducksoftware.integration.maven.logging.MavenLogger;

@Mojo(name = "createHubOutput", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PACKAGE, aggregator = true)
public class BuildInfoFileGenerator extends AbstractMojo {
	private static final String MSG_FILE_TO_GENERATE = "File to generate: ";
	private static final String EXCEPTION_MSG_FILE_NOT_CREATED = "Could not generate bdio file";

	@Parameter(defaultValue = PluginConstants.PARAM_PROJECT, readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = PluginConstants.PARAM_SESSION, readonly = true, required = true)
	private MavenSession session;

	@Parameter(defaultValue = PluginConstants.PARAM_TARGET_DIR, readonly = true)
	private File target;

	@Component
	private DependencyGraphBuilder dependencyGraphBuilder;

	private final MavenLogger logger = new MavenLogger(getLog());
	private final PluginHelper helper = new PluginHelper();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		logger.info("projects...");
		final MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor(dependencyGraphBuilder,
				session);
		final String pluginTaskString = "BlackDuck Software " + helper.getBDIOFileName(project) + " file generation";
		logger.info(pluginTaskString + " starting...");
		final DependencyNode rootNode = mavenDependencyExtractor.getRootDependencyNode(project);
		for (final MavenProject moduleProject : project.getCollectedProjects()) {
			final DependencyNode moduleRootNode = mavenDependencyExtractor.getRootDependencyNode(moduleProject);
			rootNode.getChildren().addAll(moduleRootNode.getChildren());
		}

		createBDIOFile(rootNode);
		logger.info("..." + pluginTaskString + " finished");
	}

	private void createBDIOFile(final DependencyNode rootDependencyNode) throws MojoExecutionException {
		try {
			final File file = new File(target, helper.getBDIOFileName(project));
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

}
