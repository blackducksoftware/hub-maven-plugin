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
package com.blackducksoftware.integration.maven.logging;

import org.apache.maven.plugin.logging.Log;

import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.blackducksoftware.integration.hub.logging.LogLevel;

public class MavenLogger extends IntLogger {

	private final Log log;

	public MavenLogger(final Log log) {
		this.log = log;
	}

	@Override
	public void info(final String txt) {
		log.info(txt);
	}

	@Override
	public void error(final Throwable t) {
		log.error(t);
	}

	@Override
	public void error(final String txt, final Throwable t) {
		log.error(txt, t);
	}

	@Override
	public void error(final String txt) {
		log.error(txt);
	}

	@Override
	public void warn(final String txt) {
		log.warn(txt);
	}

	@Override
	public void trace(final String txt) {
		log.debug(txt);
	}

	@Override
	public void trace(final String txt, final Throwable t) {
		log.debug(txt, t);
	}

	@Override
	public void debug(final String txt) {
		log.debug(txt);
	}

	@Override
	public void debug(final String txt, final Throwable t) {
		log.debug(txt, t);
	}

	@Override
	public void setLogLevel(final LogLevel logLevel) {
		// does not provide a means to change the log level
	}

	@Override
	public LogLevel getLogLevel() {
		if (log.isDebugEnabled()) {
			return LogLevel.DEBUG;
		} else if (log.isInfoEnabled()) {
			return LogLevel.INFO;
		} else if (log.isWarnEnabled()) {
			return LogLevel.WARN;
		} else if (log.isErrorEnabled()) {
			return LogLevel.ERROR;
		}
		return null; // unknown
	}
}
