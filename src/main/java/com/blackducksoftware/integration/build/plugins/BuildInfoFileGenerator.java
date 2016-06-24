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
package com.blackducksoftware.integration.build.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;

import com.blackducksoftware.integration.build.bdio.BdioConverter;
import com.blackducksoftware.integration.build.bdio.CommonBomFormatter;
import com.blackducksoftware.integration.build.bdio.Constants;
import com.blackducksoftware.integration.build.bdio.Gav;

@Mojo(name = "createHubOutput", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PACKAGE)
public class BuildInfoFileGenerator extends AbstractMojo {
	private static final String MSG_FILE_TO_GENERATE = "File to generate: ";
	private static final String EXCEPTION_MSG_FILE_NOT_CREATED = "Could not generate bdio file";
	private static final String EXCEPTION_MSG_DEPENDENCY_NODE_NULL = "Dependency graph is null";
	private static final String EXCEPTION_MSG_NO_DEPENDENCY_GRAPH = "Cannot build the dependency graph.";

	@Parameter(defaultValue = PluginConstants.PARAM_PROJECT, readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = PluginConstants.PARAM_SESSION, readonly = true, required = true)
	private MavenSession session;

	@Parameter(defaultValue = PluginConstants.PARAM_TARGET_DIR, readonly = true)
	private File target;

	@Component
	private DependencyGraphBuilder dependencyGraphBuilder;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final String pluginTaskString = "BlackDuck Software " + project.getName() + Constants.BDIO_FILE_SUFFIX
				+ " file generation";
		getLog().info(pluginTaskString + " starting...");
		final DependencyNode rootNode = getRootDependencyNode();
		createBDIOFile(rootNode);
		getLog().info(pluginTaskString + " finished...");
	}

	private DependencyNode getRootDependencyNode() throws MojoExecutionException {
		DependencyNode rootNode = null;
		final ProjectBuildingRequest buildRequest = new DefaultProjectBuildingRequest(
				session.getProjectBuildingRequest());
		buildRequest.setProject(project);
		buildRequest.setResolveDependencies(true);

		try {
			rootNode = dependencyGraphBuilder.buildDependencyGraph(buildRequest, null);
		} catch (final DependencyGraphBuilderException ex) {
			throw new MojoExecutionException(EXCEPTION_MSG_NO_DEPENDENCY_GRAPH, ex);
		}
		return rootNode;
	}

	private void createBDIOFile(final DependencyNode rootNode) throws MojoExecutionException {
		if (rootNode == null) {
			throw new MojoExecutionException(EXCEPTION_MSG_DEPENDENCY_NODE_NULL);
		} else {
			final CollectingDependencyNodeVisitor visitor = new CollectingDependencyNodeVisitor();
			rootNode.accept(visitor);
			for (final DependencyNode node : visitor.getNodes()) {
				getLog().info(node.toNodeString());
			}

			try {
				final Gav projectGav = new Gav(project.getGroupId(), project.getArtifactId(), project.getVersion());
				final File file = new File(target, projectGav.getArtifactId() + Constants.BDIO_FILE_SUFFIX);
				getLog().info(MSG_FILE_TO_GENERATE + file.getCanonicalPath());

				try (final OutputStream outputStream = new FileOutputStream(file)) {
					final com.blackducksoftware.integration.build.bdio.DependencyNode root = createCommonDependencyNode(
							rootNode);
					final BdioConverter bdioConverter = new BdioConverter();
					final CommonBomFormatter commonBomFormatter = new CommonBomFormatter(bdioConverter);
					commonBomFormatter.writeProject(outputStream, project.getName(), root);
				}
			} catch (final IOException e) {
				throw new MojoExecutionException(EXCEPTION_MSG_FILE_NOT_CREATED, e);
			}
		}
	}

	private com.blackducksoftware.integration.build.bdio.DependencyNode createCommonDependencyNode(
			final DependencyNode mavenDependencyNode) {
		final String groupId = mavenDependencyNode.getArtifact().getGroupId();
		final String artifactId = mavenDependencyNode.getArtifact().getArtifactId();
		final String version = mavenDependencyNode.getArtifact().getVersion();
		final Gav gav = new Gav(groupId, artifactId, version);
		final List<com.blackducksoftware.integration.build.bdio.DependencyNode> children = new ArrayList<>();
		for (final DependencyNode child : mavenDependencyNode.getChildren()) {
			children.add(createCommonDependencyNode(child));
		}
		return new com.blackducksoftware.integration.build.bdio.DependencyNode(gav, children);
	}

}
