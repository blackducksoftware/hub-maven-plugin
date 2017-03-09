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
package com.blackducksoftware.integration.maven.goal;

import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.BOM_WAIT_ERROR;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.BUILD_TOOL_CONFIGURATION_ERROR;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.BUILD_TOOL_STEP;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CHECK_POLICIES_ERROR;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CHECK_POLICIES_FINISHED;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CHECK_POLICIES_STARTING;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CREATE_FLAT_DEPENDENCY_LIST_ERROR;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CREATE_FLAT_DEPENDENCY_LIST_FINISHED;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CREATE_FLAT_DEPENDENCY_LIST_STARTING;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CREATE_HUB_OUTPUT_ERROR;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CREATE_HUB_OUTPUT_FINISHED;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CREATE_HUB_OUTPUT_STARTING;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CREATE_REPORT_FINISHED;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.CREATE_REPORT_STARTING;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.DEPLOY_HUB_OUTPUT_ERROR;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.DEPLOY_HUB_OUTPUT_FINISHED;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.DEPLOY_HUB_OUTPUT_STARTING;
import static com.blackducksoftware.integration.hub.buildtool.BuildToolConstants.FAILED_TO_CREATE_REPORT;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.buildtool.BuildToolHelper;
import com.blackducksoftware.integration.hub.buildtool.DependencyNode;
import com.blackducksoftware.integration.hub.buildtool.FlatDependencyListWriter;
import com.blackducksoftware.integration.hub.buildtool.bdio.BdioDependencyWriter;
import com.blackducksoftware.integration.hub.dataservice.policystatus.PolicyStatusDescription;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.model.enumeration.VersionBomPolicyStatusOverallStatusEnum;
import com.blackducksoftware.integration.hub.model.view.VersionBomPolicyStatusView;
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.log.Slf4jIntLogger;
import com.blackducksoftware.integration.maven.MavenDependencyExtractor;
import com.blackducksoftware.integration.maven.PluginConstants;

@Mojo(name = BUILD_TOOL_STEP, defaultPhase = LifecyclePhase.PACKAGE)
public class BuildBOMGoal extends AbstractMojo {
    public final Logger logger = LoggerFactory.getLogger(getClass());

    private BuildToolHelper BUILD_TOOL_HELPER;

    @Parameter(defaultValue = PluginConstants.PARAM_PROJECT, readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = PluginConstants.PARAM_SESSION, readonly = true, required = true)
    private MavenSession session;

    @Parameter(property = "hub.ignore.failure", defaultValue = "false")
    private boolean hubIgnoreFailure;

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

    @Parameter(property = "hub.create.flat.list")
    private boolean createFlatDependencyList;

    @Parameter(property = "hub.create.bdio", defaultValue = "true")
    private boolean createHubBdio;

    @Parameter(property = "hub.deploy.bdio", defaultValue = "true")
    private boolean deployHubBdio;

    @Parameter(property = "hub.create.report")
    private boolean createHubReport;

    @Parameter(property = "hub.check.policies")
    private boolean checkPolicies;

    @Parameter(property = "hub.output.directory", defaultValue = PluginConstants.PARAM_TARGET_DIR)
    private File outputDirectory;

    @Parameter(property = "hub.scan.timeout", defaultValue = "300")
    private int hubScanTimeout;

    @Parameter(property = "included.scopes", defaultValue = "compile")
    private String includedScopes;

    @Parameter(property = "excluded.modules", defaultValue = "")
    private String excludedModules;

    @Component
    private DependencyGraphBuilder dependencyGraphBuilder;

    private HubServicesFactory services;

    private boolean waitedForHub;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            performGoal();
        } catch (final Exception e) {
            if (isHubIgnoreFailure()) {
                logger.warn(String.format(
                        "Your task has failed: %s. Build will NOT be failed due to hub.ignore.failure being true.",
                        e.getMessage()));
            } else {
                throw e;
            }
        }
    }

    private void performGoal() throws MojoExecutionException, MojoFailureException {
        try {
            BUILD_TOOL_HELPER = new BuildToolHelper(new Slf4jIntLogger(logger));

            if (getCreateFlatDependencyList()) {
                createFlatDependencyList();
            }
            if (getCreateHubBdio()) {
                createHubBDIO();
            }
            if (getDeployHubBdio()) {
                deployHubBDIO();
            }
            if (getCreateHubReport()) {
                createHubReport();
            }
            if (getCheckPolicies()) {
                checkHubPolicies();
            }
        } catch (final Exception e) {
            if (hubIgnoreFailure) {
                logger.error(e.getMessage(), e);
            } else {
                throw e;
            }
        }
    }

    private RestConnection getRestConnection(final HubServerConfig hubServerConfig) throws EncryptionException {
        final Slf4jIntLogger intLogger = new Slf4jIntLogger(logger);
        final RestConnection restConnection = new CredentialsRestConnection(intLogger, hubServerConfig.getHubUrl(),
                hubServerConfig.getGlobalCredentials().getUsername(), hubServerConfig.getGlobalCredentials().getDecryptedPassword(),
                hubServerConfig.getTimeout());
        restConnection.proxyHost = hubServerConfig.getProxyInfo().getHost();
        restConnection.proxyPort = hubServerConfig.getProxyInfo().getPort();
        restConnection.proxyNoHosts = hubServerConfig.getProxyInfo().getIgnoredProxyHosts();
        restConnection.proxyUsername = hubServerConfig.getProxyInfo().getUsername();
        restConnection.proxyPassword = hubServerConfig.getProxyInfo().getDecryptedPassword();
        return restConnection;
    }

    private HubServicesFactory getHubServicesFactory() throws MojoFailureException {
        if (services == null) {
            final RestConnection restConnection;
            try {
                final HubServerConfig hubServerConfig = getHubServerConfigBuilder().build();
                restConnection = getRestConnection(hubServerConfig);
            } catch (final IllegalArgumentException e) {
                throw new MojoFailureException(String.format(BUILD_TOOL_CONFIGURATION_ERROR, e.getMessage()), e);
            } catch (final EncryptionException e) {
                throw new MojoFailureException(String.format(BUILD_TOOL_CONFIGURATION_ERROR, e.getMessage()), e);
            }
            services = new HubServicesFactory(restConnection);
        }
        return services;
    }

    private void waitForHub() throws MojoFailureException, MojoExecutionException {
        if (getDeployHubBdio() && !waitedForHub) {
            try {
                BUILD_TOOL_HELPER.waitForHub(getHubServicesFactory(), getHubProjectName(), getHubVersionName(), getHubScanTimeout());
                waitedForHub = true;
            } catch (final IntegrationException e) {
                throw new MojoExecutionException(String.format(BOM_WAIT_ERROR, e.getMessage()), e);
            }
        }
    }

    private void createFlatDependencyList() throws MojoExecutionException, MojoFailureException {
        logger.info(String.format(CREATE_FLAT_DEPENDENCY_LIST_STARTING, getFlatFilename()));

        try {
            final MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor(getExcludedModules(), getIncludedScopes());
            final DependencyNode rootNode = mavenDependencyExtractor.getRootDependencyNode(getDependencyGraphBuilder(), getSession(), getProject(),
                    getHubProjectName(),
                    getHubVersionName());

            BUILD_TOOL_HELPER.createFlatOutput(rootNode,
                    getHubProjectName(), getHubVersionName(), getOutputDirectory());
        } catch (final IOException e) {
            throw new MojoFailureException(String.format(CREATE_FLAT_DEPENDENCY_LIST_ERROR, e.getMessage()), e);
        }

        logger.info(String.format(CREATE_FLAT_DEPENDENCY_LIST_FINISHED, getFlatFilename()));
    }

    private void createHubBDIO() throws MojoExecutionException, MojoFailureException {
        logger.info(String.format(CREATE_HUB_OUTPUT_STARTING, getBdioFilename()));

        try {
            final MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor(getExcludedModules(), getIncludedScopes());
            final DependencyNode rootNode = mavenDependencyExtractor.getRootDependencyNode(getDependencyGraphBuilder(), getSession(), getProject(),
                    getHubProjectName(),
                    getHubVersionName());

            BUILD_TOOL_HELPER.createHubOutput(rootNode, getProject().getArtifactId(), getHubProjectName(),
                    getHubVersionName(), getOutputDirectory());
        } catch (final IOException e) {
            throw new MojoFailureException(String.format(CREATE_HUB_OUTPUT_ERROR, e.getMessage()), e);
        }

        logger.info(String.format(CREATE_HUB_OUTPUT_FINISHED, getBdioFilename()));
    }

    private void deployHubBDIO() throws MojoExecutionException, MojoFailureException {
        logger.info(String.format(DEPLOY_HUB_OUTPUT_STARTING, getBdioFilename()));

        try {
            BUILD_TOOL_HELPER.deployHubOutput(getHubServicesFactory(), getOutputDirectory(),
                    getProject().getArtifactId());
        } catch (IntegrationException | IllegalArgumentException e) {
            throw new MojoFailureException(String.format(DEPLOY_HUB_OUTPUT_ERROR, e.getMessage()), e);
        }
        logger.info(String.format(DEPLOY_HUB_OUTPUT_FINISHED, getBdioFilename()));
    }

    private void createHubReport() throws MojoExecutionException, MojoFailureException {
        logger.info(String.format(CREATE_REPORT_STARTING, getBdioFilename()));
        waitForHub();
        final File reportOutput = new File(getOutputDirectory(), "report");
        try {
            BUILD_TOOL_HELPER.createRiskReport(getHubServicesFactory(), reportOutput, getHubProjectName(), getHubVersionName(), getHubScanTimeout());
        } catch (final IntegrationException e) {
            throw new MojoFailureException(String.format(FAILED_TO_CREATE_REPORT, e.getMessage()), e);
        }
        logger.info(String.format(CREATE_REPORT_FINISHED, getBdioFilename()));
    }

    private void checkHubPolicies() throws MojoExecutionException, MojoFailureException {
        logger.info(String.format(CHECK_POLICIES_STARTING, getBdioFilename()));
        waitForHub();
        try {
            final VersionBomPolicyStatusView policyStatusItem = BUILD_TOOL_HELPER.checkPolicies(getHubServicesFactory(), getHubProjectName(),
                    getHubVersionName());
            handlePolicyStatusItem(policyStatusItem);
        } catch (IllegalArgumentException | IntegrationException e) {
            throw new MojoFailureException(String.format(CHECK_POLICIES_ERROR, e.getMessage()), e);
        }

        logger.info(String.format(CHECK_POLICIES_FINISHED, getBdioFilename()));
    }

    public void handlePolicyStatusItem(final VersionBomPolicyStatusView policyStatusItem) throws MojoFailureException {
        final PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(policyStatusItem);
        final String policyStatusMessage = policyStatusDescription.getPolicyStatusMessage();
        logger.info(policyStatusMessage);
        if (VersionBomPolicyStatusOverallStatusEnum.IN_VIOLATION == policyStatusItem.getOverallStatus()) {
            throw new MojoFailureException(policyStatusMessage);
        }
    }

    public String getBdioFilename() {
        return BdioDependencyWriter.getFilename(getProject().getArtifactId());
    }

    public String getFlatFilename() {
        return FlatDependencyListWriter.getFilename(getProject().getArtifactId());
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

    public String getHubProjectName() {
        if (StringUtils.isNotBlank(hubProjectName)) {
            return hubProjectName;
        }

        return project.getArtifactId();
    }

    public String getHubVersionName() {
        if (StringUtils.isNotBlank(hubVersionName)) {
            return hubVersionName;
        }

        return project.getVersion();
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

    public void setHubProjectName(final String hubProjectName) {
        this.hubProjectName = hubProjectName;
    }

    public void setHubVersionName(final String hubVersionName) {
        this.hubVersionName = hubVersionName;
    }

    public String getHubUrl() {
        if (StringUtils.isBlank(hubUrl)) {
            return getDeprecatedProperty("hub-url");
        }
        return hubUrl;
    }

    public void setHubUrl(final String hubUrl) {
        this.hubUrl = hubUrl;
    }

    public String getHubUsername() {
        if (StringUtils.isBlank(hubUsername)) {
            return getDeprecatedProperty("hub-user");
        }
        return hubUsername;
    }

    public void setHubUsername(final String hubUsername) {
        this.hubUsername = hubUsername;
    }

    public String getHubPassword() {
        if (StringUtils.isBlank(hubPassword)) {
            return getDeprecatedProperty("hub-password");
        }
        return hubPassword;
    }

    public void setHubPassword(final String hubPassword) {
        this.hubPassword = hubPassword;
    }

    public int getHubTimeout() {
        final String deprecatedProperty = getDeprecatedProperty("hub-timeout");
        if (StringUtils.isNotBlank(deprecatedProperty)) {
            return NumberUtils.toInt(deprecatedProperty);
        }
        return hubTimeout;
    }

    public void setHubTimeout(final int hubTimeout) {
        this.hubTimeout = hubTimeout;
    }

    public String getHubProxyHost() {
        if (StringUtils.isBlank(hubProxyHost)) {
            return getDeprecatedProperty("hub-proxy-host");
        }
        return hubProxyHost;
    }

    public void setHubProxyHost(final String hubProxyHost) {
        this.hubProxyHost = hubProxyHost;
    }

    public String getHubProxyPort() {
        if (StringUtils.isBlank(hubProxyPort)) {
            return getDeprecatedProperty("hub-proxy-port");
        }
        return hubProxyPort;
    }

    public void setHubProxyPort(final String hubProxyPort) {
        this.hubProxyPort = hubProxyPort;
    }

    public String getHubNoProxyHosts() {
        if (StringUtils.isBlank(hubNoProxyHosts)) {
            return getDeprecatedProperty("hub-proxy-no-hosts");
        }
        return hubNoProxyHosts;
    }

    public void setHubNoProxyHosts(final String hubNoProxyHosts) {
        this.hubNoProxyHosts = hubNoProxyHosts;
    }

    public String getHubProxyUsername() {
        if (StringUtils.isBlank(hubProxyUsername)) {
            return getDeprecatedProperty("hub-proxy-user");
        }
        return hubProxyUsername;
    }

    public void setHubProxyUsername(final String hubProxyUsername) {
        this.hubProxyUsername = hubProxyUsername;
    }

    public String getHubProxyPassword() {
        if (StringUtils.isBlank(hubProxyPassword)) {
            return getDeprecatedProperty("hub-proxy-password");
        }
        return hubProxyPassword;
    }

    public void setHubProxyPassword(final String hubProxyPassword) {
        this.hubProxyPassword = hubProxyPassword;
    }

    public boolean getCreateHubReport() {
        return createHubReport;
    }

    public void setCreateHubReport(final boolean createHubReport) {
        this.createHubReport = createHubReport;
    }

    public int getHubScanTimeout() {

        return hubScanTimeout;
    }

    public void setHubScanTimeout(final int hubScanTimeout) {
        this.hubScanTimeout = hubScanTimeout;
    }

    public String getIncludedScopes() {
        return includedScopes;
    }

    public void setIncludedScopes(final String includedScopes) {
        this.includedScopes = includedScopes;
    }

    public String getExcludedModules() {
        return excludedModules;
    }

    public void setExcludedModules(final String excludedModules) {
        this.excludedModules = excludedModules;
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

    private String getDeprecatedProperty(final String key) {
        return getProject().getProperties().getProperty(key);
    }

    public boolean getCreateFlatDependencyList() {
        return createFlatDependencyList;
    }

    public void setCreateFlatDependencyList(final boolean createFlatDependencyList) {
        this.createFlatDependencyList = createFlatDependencyList;
    }

    public boolean getCreateHubBdio() {
        return createHubBdio;
    }

    public void setCreateHubBdio(final boolean createHubBdio) {
        this.createHubBdio = createHubBdio;
    }

    public boolean getDeployHubBdio() {
        return deployHubBdio;
    }

    public void setDeployHubBdio(final boolean deployHubBdio) {
        this.deployHubBdio = deployHubBdio;
    }

    public boolean getCheckPolicies() {
        return checkPolicies;
    }

    public void setCheckPolicies(final boolean checkPolicies) {
        this.checkPolicies = checkPolicies;
    }

}
