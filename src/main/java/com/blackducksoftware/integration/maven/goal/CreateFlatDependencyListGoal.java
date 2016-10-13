package com.blackducksoftware.integration.maven.goal;

import static com.blackducksoftware.integration.build.Constants.CREATE_FLAT_DEPENDENCY_LIST;
import static com.blackducksoftware.integration.build.Constants.CREATE_FLAT_DEPENDENCY_LIST_ERROR;
import static com.blackducksoftware.integration.build.Constants.CREATE_FLAT_DEPENDENCY_LIST_FINISHED;
import static com.blackducksoftware.integration.build.Constants.CREATE_FLAT_DEPENDENCY_LIST_STARTING;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = CREATE_FLAT_DEPENDENCY_LIST, requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PACKAGE, aggregator = true)
public class CreateFlatDependencyListGoal extends HubMojo {
	@Override
	public void performGoal() throws MojoExecutionException, MojoFailureException {
		logger.info(String.format(CREATE_FLAT_DEPENDENCY_LIST_STARTING, getFlatFilename()));

		try {
			PLUGIN_HELPER.createFlatOutput(getProject(), getSession(), getDependencyGraphBuilder(),
					getOutputDirectory(), getFlatFilename(), getHubProject(), getHubVersion());
		} catch (final IOException e) {
			throw new MojoFailureException(String.format(CREATE_FLAT_DEPENDENCY_LIST_ERROR, e.getMessage()), e);
		}

		logger.info(String.format(CREATE_FLAT_DEPENDENCY_LIST_FINISHED, getFlatFilename()));
	}

}
