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
                    getOutputDirectory(), getHubProjectName(), getHubVersionName());
        } catch (final IOException e) {
            throw new MojoFailureException(String.format(CREATE_FLAT_DEPENDENCY_LIST_ERROR, e.getMessage()), e);
        }

        logger.info(String.format(CREATE_FLAT_DEPENDENCY_LIST_FINISHED, getFlatFilename()));
    }

}
