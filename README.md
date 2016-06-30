## Overview ##
This plugin provides ability to generate a [Black Duck I/O](https://github.com/blackducksoftware/bdio) formatted file containing the dependency information gathered from the Maven project. The file is generated in the target folder of the project. This plugin also has the ability to upload the [Black Duck I/O](https://github.com/blackducksoftware/bdio) file up to the hub to create a code location in the hub. In order to generate the file and upload the contents to the hub the pom file must have a section for this plugin and execute goals specific to this plugin.

Goals:

* createHubOutput - generates the [Black Duck I/O](https://github.com/blackducksoftware/bdio) file
* deployHubOutput - uploads the file to the hub server

## Build ##
[![Build Status](https://travis-ci.org/blackducksoftware/hub-maven-plugin.svg?branch=master)](https://travis-ci.org/blackducksoftware/hub-maven-plugin)
[![Coverage Status](https://coveralls.io/repos/github/blackducksoftware/hub-maven-plugin/badge.svg?branch=master)](https://coveralls.io/github/blackducksoftware/hub-maven-plugin?branch=master)

## Where can I get the latest release? ##
You can download the latest source from GitHub: https://github.com/blackducksoftware/hub-maven-plugin. 

## Repository Configuration
In the repositories portion of the POM file add the repositories for the plugin and the [Black Duck I/O](https://github.com/blackducksoftware/bdio) dependencies:
```
    <repository>
        <id>bds-int-public</id>
        <url>https://updates.suite.blackducksoftware.com/integrations/</url>
    </repository>
```
## Plugin Configuration ##
In the plugins portion of the POM file add the following hub-maven-plugin configuration:
```
    <build>
       <plugins>
           <plugin>
               <groupId>com.blackducksoftware.integration</groupId>
               <artifactId>hub-maven-plugin</artifactId>
               <version>1.0.0</version>
               <executions>
                   <execution>
                       <phase>package</phase>
                       <goals>
                           <goal>createHubOutput</goal>
                           <goal>deployHubOutput</goal>
                       </goals>
                   </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

## License ##
Apache License 2.0
