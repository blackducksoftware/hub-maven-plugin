## Overview ##
This plugin provides ability to generate a [Black Duck I/O](https://github.com/blackducksoftware/bdio) formatted file containing the dependency information gathered from the Maven project. The file is generated in the target folder of the project. This plugin also has the ability to upload the [Black Duck I/O](https://github.com/blackducksoftware/bdio) file up to the hub to create a code location in the hub. In order to generate the file and upload the contents to the hub the Maven pom file must have a section for this plugin and execute goals specific to this plugin.  A developer or release engineer will have to update the projects pom.xml file(s) to utilize this Maven plugin.  You must be familiar with Maven, POM files, Maven settings, and Maven profiles to use this plugin.

Goals:

* createHubOutput - generates the [Black Duck I/O](https://github.com/blackducksoftware/bdio) file
* deployHubOutput - uploads the file to the hub server  (Supported in a future version of both the HUB and this Maven plugin)

In order to use this plugin you need to perform the following:

1. Update the settings.xml file to contain the HUB server configuration information in the active profile used.
2. Update the pom.xml file of the project to utilize the Maven plugin to generate the output file.
3. Build your Maven project.  Ensure the package phase of the build is executed.

## Build ##
[![Build Status](https://travis-ci.org/blackducksoftware/hub-maven-plugin.svg?branch=master)](https://travis-ci.org/blackducksoftware/hub-maven-plugin)
[![Coverage Status](https://coveralls.io/repos/github/blackducksoftware/hub-maven-plugin/badge.svg?branch=master)](https://coveralls.io/github/blackducksoftware/hub-maven-plugin?branch=master)

## Where can I get the latest release? ##
You can download the latest release from the Maven Central repository: http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22hub-maven-plugin%22

## License ##
Apache License 2.0 
