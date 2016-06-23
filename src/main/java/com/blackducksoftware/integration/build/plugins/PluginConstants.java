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

public class PluginConstants {
	public static final String DIRECTORY_TARGET = "target";

	public static final String PARAM_PROJECT = "${project}";
	public static final String PARAM_SESSION = "${session}";
	public static final String PARAM_TARGET_DIR = "${project.build.directory}";
	public static final String PARAM_HUB_URL = "${hub-url}";
	public static final String PARAM_HUB_USER = "${hub-user}";
	public static final String PARAM_HUB_PASSWORD = "${hub-password}";
	public static final String PARAM_HUB_TIMEOUT = "${hub-timeout}";
	public static final String PARAM_HUB_PROXY_HOST = "${hub-proxy-host}";
	public static final String PARAM_HUB_PROXY_PORT = "${hub-proxy-port}";
	public static final String PARAM_HUB_PROXY_NO_HOSTS = "${hub-proxy-no-hosts}";
	public static final String PARAM_HUB_PROXY_USER = "${hub-proxy-user}";
	public static final String PARAM_HUB_PROXY_PASSWORD = "${hub-proxy-password}";

}
