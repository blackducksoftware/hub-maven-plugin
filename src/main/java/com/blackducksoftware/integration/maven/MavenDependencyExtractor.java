/**
 * hub-maven-plugin
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;

import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode;
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.MavenExternalId;

public class MavenDependencyExtractor {
    private static final String EXCEPTION_MSG_NO_DEPENDENCY_GRAPH = "Cannot build the dependency graph.";

    private final Set<String> excludedModules = new HashSet<>();

    private final Set<String> includedScopes = new HashSet<>();

    public MavenDependencyExtractor(final String excludedModules, final String includedScopes) {
        if (StringUtils.isNotBlank(excludedModules)) {
            final String[] pieces = excludedModules.split(",");
            for (final String piece : pieces) {
                if (StringUtils.isNotBlank(piece)) {
                    this.excludedModules.add(piece);
                }
            }
        }

        if (StringUtils.isNotBlank(includedScopes)) {
            final String[] pieces = includedScopes.split(",");
            for (final String piece : pieces) {
                if (StringUtils.isNotBlank(piece)) {
                    this.includedScopes.add(piece);
                }
            }
        }
    }

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
        final MavenExternalId projectGav = new MavenExternalId(groupId, artifactId, version);

        final Set<DependencyNode> children = new LinkedHashSet<>();
        final DependencyNode root = new DependencyNode(projectName, versionName, projectGav, children);
        for (final org.apache.maven.shared.dependency.graph.DependencyNode child : rootNode.getChildren()) {
            if (includedScopes.contains(child.getArtifact().getScope())) {
                children.add(createCommonDependencyNode(child));
            }
        }

        for (final MavenProject moduleProject : project.getCollectedProjects()) {
            if (!excludedModules.contains(moduleProject.getArtifactId())) {
                final DependencyNode moduleRootNode = getRootDependencyNode(dependencyGraphBuilder, session, moduleProject, moduleProject.getArtifactId(),
                        moduleProject.getVersion());
                children.addAll(moduleRootNode.children);
            }
        }

        return root;
    }

    private DependencyNode createCommonDependencyNode(
            final org.apache.maven.shared.dependency.graph.DependencyNode mavenDependencyNode) {
        final MavenExternalId gav = createGavFromDependencyNode(mavenDependencyNode);
        final Set<DependencyNode> children = new LinkedHashSet<>();
        final DependencyNode dependencyNode = new DependencyNode(gav, children);

        for (final org.apache.maven.shared.dependency.graph.DependencyNode child : mavenDependencyNode.getChildren()) {
            children.add(createCommonDependencyNode(child));
        }

        return dependencyNode;
    }

    private MavenExternalId createGavFromDependencyNode(
            final org.apache.maven.shared.dependency.graph.DependencyNode mavenDependencyNode) {
        final String groupId = mavenDependencyNode.getArtifact().getGroupId();
        final String artifactId = mavenDependencyNode.getArtifact().getArtifactId();
        final String version = mavenDependencyNode.getArtifact().getVersion();

        final MavenExternalId gav = new MavenExternalId(groupId, artifactId, version);
        return gav;
    }
}
