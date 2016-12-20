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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;

import com.blackducksoftware.integration.build.DependencyNode;
import com.blackducksoftware.integration.build.Gav;

public class MavenDependencyExtractor {
    private static final String EXCEPTION_MSG_NO_DEPENDENCY_GRAPH = "Cannot build the dependency graph.";

    public DependencyNode getRootDependencyNode(final DependencyGraphBuilder dependencyGraphBuilder, final MavenSession session, final MavenProject project,
            final String projectName,
            final String versionName) throws MojoExecutionException {
        org.apache.maven.shared.dependency.graph.DependencyNode rootNode = null;
        final ProjectBuildingRequest buildRequest = new DefaultProjectBuildingRequest(
                session.getProjectBuildingRequest());
        buildRequest.setProject(project);
        buildRequest.setResolveDependencies(true);

        try {
            rootNode = dependencyGraphBuilder.buildDependencyGraph(buildRequest, null);
        } catch (final DependencyGraphBuilderException ex) {
            throw new MojoExecutionException(EXCEPTION_MSG_NO_DEPENDENCY_GRAPH, ex);
        }

        final String groupId = project.getGroupId();
        final String artifactId = project.getArtifactId();
        final String version = versionName;
        final Gav projectGav = new Gav(groupId, artifactId, version);

        final List<DependencyNode> children = new ArrayList<>();
        final DependencyNode root = new DependencyNode(projectGav, children);
        for (final org.apache.maven.shared.dependency.graph.DependencyNode child : rootNode.getChildren()) {
            children.add(createCommonDependencyNode(child));
        }

        for (final MavenProject moduleProject : project.getCollectedProjects()) {
            final DependencyNode moduleRootNode = getRootDependencyNode(dependencyGraphBuilder, session, moduleProject, moduleProject.getArtifactId(),
                    moduleProject.getVersion());
            children.addAll(moduleRootNode.getChildren());
        }

        return root;
    }

    private DependencyNode createCommonDependencyNode(
            final org.apache.maven.shared.dependency.graph.DependencyNode mavenDependencyNode) {
        final Gav gav = createGavFromDependencyNode(mavenDependencyNode);
        final List<DependencyNode> children = new ArrayList<>();
        final DependencyNode dependencyNode = new DependencyNode(gav, children);

        for (final org.apache.maven.shared.dependency.graph.DependencyNode child : mavenDependencyNode.getChildren()) {
            children.add(createCommonDependencyNode(child));
        }

        return dependencyNode;
    }

    private Gav createGavFromDependencyNode(
            final org.apache.maven.shared.dependency.graph.DependencyNode mavenDependencyNode) {
        final String groupId = mavenDependencyNode.getArtifact().getGroupId();
        final String artifactId = mavenDependencyNode.getArtifact().getArtifactId();
        final String version = mavenDependencyNode.getArtifact().getVersion();

        final Gav gav = new Gav(groupId, artifactId, version);
        return gav;
    }
}
