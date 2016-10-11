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

import java.io.File;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.maven.PluginConstants;
import com.blackducksoftware.integration.maven.PluginHelper;

@Mojo(name = PluginConstants.GOAL_CREATE_HUB_OUTPUT, requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PACKAGE, aggregator = true)
public class CreateHubOutputGoal extends AbstractMojo {
	private final Logger logger = LoggerFactory.getLogger(CreateHubOutputGoal.class);

	@Parameter(defaultValue = PluginConstants.PARAM_PROJECT, readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = PluginConstants.PARAM_SESSION, readonly = true, required = true)
	private MavenSession session;

	@Parameter(defaultValue = PluginConstants.PARAM_TARGET_DIR, readonly = true)
	private File target;

	@Component
	private DependencyGraphBuilder dependencyGraphBuilder;

	@Component
	private PluginHelper pluginHelper;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		logger.info(String.format("Black Duck Hub creating output file for: %s starting.",
				pluginHelper.getBDIOFileName(project)));

		pluginHelper.createHubOutput(project, session, target, dependencyGraphBuilder);

		logger.info(String.format("Black Duck Hub creating output file for: %s finished.",
				pluginHelper.getBDIOFileName(project)));
	}

}
