## Overview ##

*Update:* This plugin is no longer supported.  This functionality has been replaced by [Detect](https://github.com/blackducksoftware/hub-detect).


This plugin provides ability to generate a [Black Duck I/O](https://github.com/blackducksoftware/bdio) formatted file containing the dependency information gathered from the Maven project. The file is generated in the target folder of the project. This plugin also has the ability to upload the [Black Duck I/O](https://github.com/blackducksoftware/bdio) file up to the hub to create a code location in the hub. In order to generate the file and upload the contents to the hub the Maven pom file must have a section for this plugin and execute goals specific to this plugin.  A developer or release engineer will have to update the projects pom.xml file(s) to utilize this Maven plugin.  You must be familiar with Maven, POM files, Maven settings, and Maven profiles to use this plugin.

## Build ##
[![Build Status](https://travis-ci.org/blackducksoftware/hub-maven-plugin.svg?branch=master)](https://travis-ci.org/blackducksoftware/hub-maven-plugin)
[![Coverage Status](https://coveralls.io/repos/github/blackducksoftware/hub-maven-plugin/badge.svg?branch=master)](https://coveralls.io/github/blackducksoftware/hub-maven-plugin?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Black Duck Security Risk](https://copilot.blackducksoftware.com/github/groups/blackducksoftware/locations/hub-maven-plugin/public/results/branches/master/badge-risk.svg)](https://copilot.blackducksoftware.com/github/groups/blackducksoftware/locations/hub-maven-plugin/public/results/branches/master)

## Where can I get the latest release? ##
You can download the latest release from the Maven Central repository: http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22hub-maven-plugin%22

## Documentation ##
Please see our public [Black Duck Confluence](https://blackducksoftware.atlassian.net/wiki/display/INTDOCS/)
