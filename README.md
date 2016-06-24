## Overview ##
Hub plugin for Maven. This plugin provides ability to generate a Black Duck I/O formatted file containing the dependency information gathered from the Maven project.  The file is generated in the target folder of the project.  This plugin also has the ability to upload the Black Duck I/O file up to the hub to create a code location in the hub.  In order to generate the file and upload the contents to the hub the pom file must have a section for this plugin and execute goals specific to this plugin.

Goals:

* createHubOutput - generates the file in the target folder during the packaging phase.
* deployHubOutput - uploads the Black Duck I/O file up to the hub server.

## Repository Configuration
In the repositories portion of the POM file add the repositories for the plugin and the Black Duck I/O dependencies:
```
    <repository>
      <snapshots>
        <updatePolicy>always</updatePolicy>
      </snapshots>
      <id>bds-int-public</id>
      <url>https://updates.suite.blackducksoftware.com/integrations/</url>
    </repository>
```
```    
    <repository>
      <snapshots>
        <updatePolicy>always</updatePolicy>
      </snapshots>
      <id>oss.jfrog.org</id>
      <url>https://oss.jfrog.org/artifactory/oss-snapshot-local/</url>
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
               <version>1.0-SNAPSHOT</version>
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