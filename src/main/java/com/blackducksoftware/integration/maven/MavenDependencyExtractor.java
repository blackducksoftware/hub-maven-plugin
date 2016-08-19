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
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;

import com.blackducksoftware.integration.build.bdio.DependencyNode;
import com.blackducksoftware.integration.build.bdio.Gav;

public class MavenDependencyExtractor {
	private static final String EXCEPTION_MSG_NO_DEPENDENCY_GRAPH = "Cannot build the dependency graph.";

	private final DependencyGraphBuilder dependencyGraphBuilder;
	private final MavenSession session;

	public MavenDependencyExtractor(final DependencyGraphBuilder dependencyGraphBuilder, final MavenSession session) {
		this.dependencyGraphBuilder = dependencyGraphBuilder;
		this.session = session;
	}

	public DependencyNode getRootDependencyNode(final MavenProject project) throws MojoExecutionException {
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

		final CollectingDependencyNodeVisitor visitor = new CollectingDependencyNodeVisitor();
		rootNode.accept(visitor);

		final DependencyNode rootDependencyNode = createCommonDependencyNode(rootNode);
		return rootDependencyNode;
	}

	private DependencyNode createCommonDependencyNode(
			final org.apache.maven.shared.dependency.graph.DependencyNode mavenDependencyNode) {
		final Gav gav = createGavFromDependencyNode(mavenDependencyNode);
		final List<com.blackducksoftware.integration.build.bdio.DependencyNode> children = new ArrayList<>();
		final DependencyNode dependencyNode = new DependencyNode(gav, children);

		for (final org.apache.maven.shared.dependency.graph.DependencyNode child : mavenDependencyNode.getChildren()) {
			children.add(createCommonDependencyNode(child));
		}

		return dependencyNode;
	}

	public Gav createGavFromDependencyNode(
			final org.apache.maven.shared.dependency.graph.DependencyNode mavenDependencyNode) {
		final String groupId = mavenDependencyNode.getArtifact().getGroupId();
		final String artifactId = mavenDependencyNode.getArtifact().getArtifactId();
		final String version = mavenDependencyNode.getArtifact().getVersion();

		final Gav gav = new Gav(groupId, artifactId, version);
		return gav;
	}

}
