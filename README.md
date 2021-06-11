# Jenkins Pipeline Shared Libraries

This repository contains shared libraries used across different KIE Jenkins pipeline scripts. 

# Development
Your scripts should be located in `/vars` folder.

## How to cover with unit tests
Once your groovy methods/script files are implemented you should cover them with Spock tests.
Those tests are located in `/test/vars` folder. Remember you should add Jenkins plugins used by the script as a dependency in the `pom.xml` file in case it is not present, like

```xml
<dependency>
    <!-- provides configFileProvider() and configFile() steps -->
    <groupId>org.jenkins-ci.plugins</groupId>
    <artifactId>config-file-provider</artifactId>
    <version>3.6.2</version>
    <scope>test</scope>
</dependency>
```
