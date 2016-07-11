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

## Settings.xml Configuration ##
Your settings.xml file used with Maven will need be updated in order to provide the plugin with the connection information for the HUB.

One of your active profiles in Maven needs to define the following properties:
```
    <properties>
        <hub-url>http://some.hub.server.com</hub-url>
        <hub-user>username</hub-user>
        <hub-password>password</hub-password>
        <hub-timeout>120</hub-timeout>
        <hub-proxy-host>some.proxy.uri</hub-proxy-host>
        <hub-proxy-port>1234</hub-proxy-port>
        <hub-proxy-no-hosts></hub-proxy-no-hosts>
        <hub-proxy-user>proxyuser</hub-proxy-user>
        <hub-proxy-password>proxypass</hub-proxy-password>
    </properties>
```
You can create a profile that contains only these properties in your settings.xml file.
```
<profile>
    <id>hub-build-info</id>
    <properties>
        <hub-url>http://some.hub.server.com</hub-url>
        <hub-user>username</hub-user>
        <hub-password>password</hub-password>
        <hub-timeout>120</hub-timeout>
        <hub-proxy-host>some.proxy.uri</hub-proxy-host>
        <hub-proxy-port>1234</hub-proxy-port>
        <hub-proxy-no-hosts></hub-proxy-no-hosts>
        <hub-proxy-user>proxyuser</hub-proxy-user>
        <hub-proxy-password>proxypass</hub-proxy-password>
    </properties>
</profile>
```
If you do create a profile to only contain the HUB configuration data, then make sure that the profile is active in order for the properties to be available to the plugin during the build of the project.

You can include the profile to the list of active profiles by editing the activeProfiles element in the XML file.
```
  <activeProfiles>
    <activeProfile>hub-build-info</activeProfile>
  </activeProfiles>
```
This would apply this profile any time Maven is executed.  If this is not the desired behavior please read the Maven documentation on profiles and configure your environment accordingly.

## POM File Configuration ##
In the plugins portion of the POM file add the hub-maven-plugin configuration.  This is shown below
```
    <build>
       <plugins>
           <plugin>
               <groupId>com.blackducksoftware.integration</groupId>
               <artifactId>hub-maven-plugin</artifactId>
               <version>1.0.1</version>
               <executions>
                   <execution>
                       <id>create-bdio-file</id>
                       <phase>package</phase>
                       <goals>
                           <goal>createHubOutput</goal>
                       </goals>
                   </execution>
                   <execution>
                       <id>deploy-bdio-file</id>
                       <phase>deploy</phase>
                       <goals>
                           <goal>deployHubOutput</goal>
                       </goals>
                   </execution>
               </executions>
            </plugin>
        </plugins>
    </build>
```

## Maven Command Line ##
If you configure the settings.xml and POM file for the project as shown in the examples above there is nothing additional needed, the plugin will be invoked.  Simply make sure the package phase occurs.  Examples of the Maven commands that invoke the plugin are shown below.
```
mvn package
```
```
mvn install
```
```
mvn deploy
```

## License ##
Apache License 2.0
