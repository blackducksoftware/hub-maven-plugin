package com.blackducksoftware.integration.maven.goal;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.maven.PluginConstants;
import com.blackducksoftware.integration.maven.PluginHelper;

@Mojo(name = PluginConstants.GOAL_CHECK_POLICIES, defaultPhase = LifecyclePhase.PACKAGE)
public class CheckPoliciesGoal extends AbstractMojo {
	private final Logger logger = LoggerFactory.getLogger(CheckPoliciesGoal.class);

	@Parameter(defaultValue = PluginConstants.PARAM_PROJECT, readonly = true, required = true)
	private MavenProject project;

	@Component
	private PluginHelper pluginHelper;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		logger.info(String.format("Black Duck Hub checking policies for: %s starting.",
				pluginHelper.getBDIOFileName(project)));

		// TODO add check policy

		logger.info(String.format("Black Duck Hub checking policies for: %s finished.",
				pluginHelper.getBDIOFileName(project)));
	}

}
