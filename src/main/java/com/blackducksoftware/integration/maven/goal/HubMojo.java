package com.blackducksoftware.integration.maven.goal;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.build.Constants;
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusEnum;
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusItem;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.dataservices.policystatus.PolicyStatusDescription;
import com.blackducksoftware.integration.maven.PluginConstants;
import com.blackducksoftware.integration.maven.PluginHelper;

public abstract class HubMojo extends AbstractMojo {
	// NOTE: getClass() is a little strange here, but we want the runtime class,
	// not HubMojo, as it is abstract.
	public final Logger logger = LoggerFactory.getLogger(getClass());

	public static final PluginHelper PLUGIN_HELPER = new PluginHelper();

	@Parameter(defaultValue = PluginConstants.PARAM_PROJECT, readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = PluginConstants.PARAM_SESSION, readonly = true, required = true)
	private MavenSession session;

	@Parameter(property = "hub.ignore.failure", defaultValue = "false")
	private boolean hubIgnoreFailure;

	@Parameter(property = "hub.output.directory", defaultValue = PluginConstants.PARAM_TARGET_DIR)
	private File outputDirectory;

	@Parameter(property = "hub.project.name")
	private String hubProjectName;

	@Parameter(property = "hub.version.name")
	private String hubVersionName;

	@Parameter(property = "hub.url")
	private String hubUrl;

	@Parameter(property = "hub.username")
	private String hubUsername;

	@Parameter(property = "hub.password")
	private String hubPassword;

	@Parameter(property = "hub.timeout", defaultValue = "120")
	private int hubTimeout;

	@Parameter(property = "hub.proxy.host")
	private String hubProxyHost;

	@Parameter(property = "hub.proxy.port")
	private String hubProxyPort;

	@Parameter(property = "hub.proxy.no.hosts")
	private String hubNoProxyHosts;

	@Parameter(property = "hub.proxy.username")
	private String hubProxyUsername;

	@Parameter(property = "hub.proxy.password")
	private String hubProxyPassword;

	@Component
	private DependencyGraphBuilder dependencyGraphBuilder;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			performGoal();
		} catch (final MojoFailureException e) {
			if (hubIgnoreFailure) {
				logger.warn(String.format(
						"Your task has failed: %s. Build will NOT be failed due to hub.ignore.failure being true.",
						e.getMessage()));
			} else {
				throw e;
			}
		}
	}

	public abstract void performGoal() throws MojoExecutionException, MojoFailureException;

	public void handlePolicyStatusItem(final PolicyStatusItem policyStatusItem) throws MojoFailureException {
		final PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(policyStatusItem);
		final String policyStatusMessage = policyStatusDescription.getPolicyStatusMessage();
		logger.info(policyStatusMessage);
		if (PolicyStatusEnum.IN_VIOLATION == policyStatusItem.getOverallStatus()) {
			throw new MojoFailureException(policyStatusMessage);
		}
	}

	public String getHubProject() {
		if (StringUtils.isNotBlank(hubProjectName)) {
			return hubProjectName;
		}

		return project.getArtifactId();
	}

	public String getHubVersion() {
		if (StringUtils.isNotBlank(hubVersionName)) {
			return hubVersionName;
		}

		return project.getVersion();
	}

	public String getBdioFilename() {
		return getHubProjectName() + Constants.BDIO_FILE_SUFFIX;
	}

	public String getFlatFilename() {
		return getHubProjectName() + Constants.FLAT_FILE_SUFFIX;
	}

	public HubServerConfigBuilder getHubServerConfigBuilder() {
		final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
		hubServerConfigBuilder.setHubUrl(getHubUrl());
		hubServerConfigBuilder.setUsername(getHubUsername());
		hubServerConfigBuilder.setPassword(getHubPassword());
		hubServerConfigBuilder.setTimeout(getHubTimeout());
		hubServerConfigBuilder.setProxyHost(getHubProxyHost());
		hubServerConfigBuilder.setProxyPort(getHubProxyPort());
		hubServerConfigBuilder.setIgnoredProxyHosts(getHubNoProxyHosts());
		hubServerConfigBuilder.setProxyUsername(getHubProxyUsername());
		hubServerConfigBuilder.setProxyPassword(getHubProxyPassword());

		return hubServerConfigBuilder;
	}

	public boolean isHubIgnoreFailure() {
		return hubIgnoreFailure;
	}

	public void setHubIgnoreFailure(final boolean hubIgnoreFailure) {
		this.hubIgnoreFailure = hubIgnoreFailure;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(final File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getHubProjectName() {
		return hubProjectName;
	}

	public void setHubProjectName(final String hubProjectName) {
		this.hubProjectName = hubProjectName;
	}

	public String getHubVersionName() {
		return hubVersionName;
	}

	public void setHubVersionName(final String hubVersionName) {
		this.hubVersionName = hubVersionName;
	}

	public String getHubUrl() {
		return hubUrl;
	}

	public void setHubUrl(final String hubUrl) {
		this.hubUrl = hubUrl;
	}

	public String getHubUsername() {
		return hubUsername;
	}

	public void setHubUsername(final String hubUsername) {
		this.hubUsername = hubUsername;
	}

	public String getHubPassword() {
		return hubPassword;
	}

	public void setHubPassword(final String hubPassword) {
		this.hubPassword = hubPassword;
	}

	public int getHubTimeout() {
		return hubTimeout;
	}

	public void setHubTimeout(final int hubTimeout) {
		this.hubTimeout = hubTimeout;
	}

	public String getHubProxyHost() {
		return hubProxyHost;
	}

	public void setHubProxyHost(final String hubProxyHost) {
		this.hubProxyHost = hubProxyHost;
	}

	public String getHubProxyPort() {
		return hubProxyPort;
	}

	public void setHubProxyPort(final String hubProxyPort) {
		this.hubProxyPort = hubProxyPort;
	}

	public String getHubNoProxyHosts() {
		return hubNoProxyHosts;
	}

	public void setHubNoProxyHosts(final String hubNoProxyHosts) {
		this.hubNoProxyHosts = hubNoProxyHosts;
	}

	public String getHubProxyUsername() {
		return hubProxyUsername;
	}

	public void setHubProxyUsername(final String hubProxyUsername) {
		this.hubProxyUsername = hubProxyUsername;
	}

	public String getHubProxyPassword() {
		return hubProxyPassword;
	}

	public void setHubProxyPassword(final String hubProxyPassword) {
		this.hubProxyPassword = hubProxyPassword;
	}

	public MavenProject getProject() {
		return project;
	}

	public MavenSession getSession() {
		return session;
	}

	public DependencyGraphBuilder getDependencyGraphBuilder() {
		return dependencyGraphBuilder;
	}

}
