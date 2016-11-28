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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
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

import com.blackducksoftware.integration.build.utils.BdioDependencyWriter;
import com.blackducksoftware.integration.build.utils.FlatDependencyListWriter;
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

    @Parameter(property = "hub.create.report", defaultValue = "true")
    private boolean createHubReport;

    @Parameter(property = "hub.output.directory", defaultValue = PluginConstants.PARAM_TARGET_DIR)
    private File outputDirectory;

    @Parameter(property = "hub.scan.started.timeout", defaultValue = "300")
    private int hubScanStartedTimeout;

    @Parameter(property = "hub.scan.finished.timeout", defaultValue = "300")
    private int hubScanFinishedTimeout;

    @Component
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            performGoal();
        } catch (final MojoFailureException e) {
            if (isHubIgnoreFailure()) {
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

    public String getBdioFilename() {
        return BdioDependencyWriter.getFilename(getHubProjectName());
    }

    public String getFlatFilename() {
        return FlatDependencyListWriter.getFilename(getHubProjectName());
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
        String deprecatedProperty = getDeprecatedProperty("hub-timeout");
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

    public void setCreateHubReport(boolean createHubReport) {
        this.createHubReport = createHubReport;
    }

    public int getHubScanStartedTimeout() {
        return hubScanStartedTimeout;
    }

    public void setHubScanStartedTimeout(final int hubScanStartedTimeout) {
        this.hubScanStartedTimeout = hubScanStartedTimeout;
    }

    public int getHubScanFinishedTimeout() {
        return hubScanFinishedTimeout;
    }

    public void setHubScanFinishedTimeout(final int hubScanFinishedTimeout) {
        this.hubScanFinishedTimeout = hubScanFinishedTimeout;
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

    private String getDeprecatedProperty(String key) {
        return getProject().getProperties().getProperty(key);
    }

}
