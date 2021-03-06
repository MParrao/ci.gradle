# ci.gradle [![Build Status](https://travis-ci.org/WASdev/ci.gradle.svg?branch=master)](https://travis-ci.org/WASdev/ci.gradle)

A Gradle plugin to support the manipulation of WebSphere Application Server Liberty Profile servers.

* [Build](#build)
* [Usage](#usage)
* [Tasks](#tasks)

## Build

Clone this repository and then, with a JRE on the path, execute the following command in the root directory.

```bash
gradlew build
```

This will download Gradle and then build the plugin `liberty-gradle-plugin-1.0-SNAPSHOT.jar` in to the `build\libs` directory. It is also possible to install the plugin in to your local Maven repository using `gradlew install`.

## Usage
###1. Configuring your dependencies
#### 1.1 Adding the ant plugin to the build script
This plugin needs the `wlp-anttasks.jar`file as dependency, this file can be downloaded from the [snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/net/wasdev/wlp/ant/wlp-anttasks/) or the [Maven central repository](http://repo1.maven.org/maven2/net/wasdev/wlp/ant/wlp-anttasks/).

The following code snippet shows an example on how to set up your build script correctly.
```groovy
buildscript {
    dependencies {
        classpath files('gradle/wlp-anttasks.jar')
    }
}
```

####1.2 Configuring the path to your WebSphere Application Server Liberty Profile installation
You need to set up the classpath to the `ws-server.jar` of your WebSphere Application Server Liberty Profile installation. This JAR is located inside `/bin/tools/` in your installation folder.

For example:
```groovy
buildscript {
    dependencies {
        classpath files('c:/wlp/bin/tools/ws-server.jar')
    }
}
```


###2. Adding the binary plugin to the build script

Within your Gradle build script, you need to set up the classpath to include the Liberty Gradle plugin. You also need to define the Maven Central repository to find the plugin or its dependencies. 

If you are using a snapshot version of the plugin make sure to define the Sonatype Nexus Snapshots repository in addition to the Maven Central repository.

Your build script should look like this:

```groovy
buildscript {
    repositories {
        mavenCentral()
        maven {
            name = 'Sonatype Nexus Snapshots'
            url = 'https://oss.sonatype.org/content/repositories/snapshots/'
        }
    }
    dependencies {
        classpath 'net.wasdev.wlp.gradle.plugins:liberty-gradle-plugin:1.0-SNAPSHOT'
    }
}
```

Alternatively, you might choose to include the plugin JAR file. For example:

```groovy
buildscript {
    dependencies {
        classpath files('gradle/liberty-gradle-plugin.jar')
        classpath files('gradle/wlp-anttasks.jar')
        classpath files('c:/wlp/bin/tools/ws-server.jar')
    }
}
```
To use the Liberty Gradle Plugin, include the following code in your build script:


```groovy
apply plugin: 'liberty'
```

##Tasks

The plugin will have made the following tasks available to your project:

| Task | Description |
| --------- | ------------ |
| installLiberty | Installs Liberty Profile from a repository. |
| libertyCreate | Creates a WebSphere Liberty Profile server. |
| libertyStart | Starts the WebSphere Liberty Profile server. |
| libertyStop | Stops the WebSphere Liberty Profile server. |
| libertyRun | Runs a WebSphere Liberty Profile server under the Gradle process. |
| libertyPackage | Generates a WebSphere Liberty Profile server archive. | 
| libertyDump | Dumps diagnostic information from the Liberty Profile server into an archive. | 
| libertyJavaDump | Dumps diagnostic information from the Liberty Profile server JVM. | 
| libertyDebug | Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777). | 
| libertyStatus | Checks the WebSphere Liberty Profile server is running. |
| libertyPackage | Generates a WebSphere Liberty Profile server archive. |
| deployWar | Deploys a WAR file to the WebSphere Liberty Profile server. |
| undeployWar | Removes a WAR file from the WebSphere Liberty Profile server. |
| installFeature | Installs a new feature in the WebSphere Liberty Profile server. |

###Extension properties
The Liberty Gradle Plugin has some properties defined in the `Liberty` closure which will let you customize the different tasks.
These properties are divided in two groups, the general properties (Which need to be set for any task excluding `installLiberty` task) and the specific ones. (Which only must be set when a specific task will be kicked off).

####**General properties**.

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| wlpDir | Location of the Liberty profile server installation. | Yes |
| outputDir |  Value of the `${wlp_output_dir}` variable. The default value is `${installDir}/usr/servers/${serverName}`.| No |
| userDir | Value of the `${wlp_user_dir}` variable. The default value is `${installDir}/usr/`. | No |
| serverName |Name of the Liberty profile server instance. The default value is `defaultServer`. | No |

#### Server Properties

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| clean | Clean all cached information on server start up. The default value is `false`. Only used with the `libertyStart` task. | No | 
| timeout | Waiting time before the server starts. The default value is 30 seconds. The unit is milliseconds. Only used with the `libertyStart` task. | No | 
| include | A comma-delimited list of values. The valid values vary depending on the task. For the `libertyPackage` task the valid values are `all`, `usr`, and `minify` and must be declared in the `packageLiberty` closure. For the `libertyDump` task the valid values are `heap`, `system`, and `thread` and must be declared in the `dumpLiberty` closure. For the `libertyJavaDump` task the valid values are `heap` and `system` and must be declared in the `javaDumpLiberty` closure. | No. |
| archive | Location of the target archive file. Only used with the `libertyPackage` or `libertyDump` tasks on their respective closures. | No |
| template | Name of the template to use when creating a new server. Only used with the `libertyCreate` task. | No |

This example shows you how to configure these properties in your script:
```groovy
apply plugin: 'liberty'

liberty {
    wlpDir = 'c:/wlp'
    serverName = 'myServer'
    userDir = 'c:/usr'
    outputDir = 'c:/usr'
    clean = true
    timeout = "10000"

    packageLiberty{
        archive = "MyServerPackage.zip"
        include = "usr"
    }
    dumpLiberty{
        archive = "C:/Dump.zip"
        include = "heap, system"
    }
    javaDumpLiberty{
        archive = "JavaDump.zip"
        include = "system"
    }
}

```

Of the plugin configuration, only the `wlpDir` property is required. The default configuration is to use a server named `defaultServer` under the `build\wlp` directory of the Gradle project.


### installLiberty task

The `installLiberty` task is used to download and install Liberty profile server. The task can download the Liberty runtime archive from a specified location (via `runtimeUrl`) or automatically resolve it from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/) based on a version. 

The Liberty license code must always be set in order to install the runtime. If you are installing Liberty from the Liberty repository, you can obtain the license code by reading the [current license](http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/8.5.5.2/lafiles/runtime/en.html) and looking for the `D/N: <license code>` line. Otherwise, download the runtime archive and execute `java -jar wlp*runtime.jar --viewLicenseInfo` command and look for the `D/N: <license code>` line.

####**Properties**

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| licenseCode | Liberty profile license code. See [above](#install-liberty-task). | Yes |
| version | Exact or wildcard version of the Liberty profile server to install. Available versions are listed in the [index.yml](http://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/index.yml) file. Only used if `runtimeUrl` is not set. The default value is `8.5.+`. | No |
| runtimeUrl | URL to the Liberty profile's `wlp*runtime.jar`. If not set, the Liberty repository will be used to find the Liberty runtime archive. | No |
| baseDir | The base installation directory. The actual installation directory of Liberty profile will be `${baseDir}/wlp`. The default value is `.` (current working directory). | No | 
| cacheDir | The directory used for caching downloaded files such as the license or `.jar` files. The default value is `${java.io.tmpdir}/wlp-cache`. | No | 
| username | Username needed for basic authentication. | No | 
| password | Password needed for basic authentication. | No | 
| maxDownloadTime | Maximum time in seconds the download can take. The default value is `0` (no maximum time). | No | 

#### Examples

```groovy
    // Install using Liberty repository
    apply plugin: 'liberty'

    liberty {

        install {
            licenseCode = "<license code>"
        }

    }

    //Install from a specific location
    apply plugin: 'liberty'

    liberty {

        install {
            licenseCode = "<license code>"
            runtimeUrl = "<url to runtime.jar>"
        }

    }

```

####**installFeature** task properties.

| Attribute | Description | Required |
| --------- | ------------ | ----------|
| featureName |Specifies the name of the Subsystem Archive (ESA file) to be installed. The value can be a feature name, a file name or a URL. | Yes |
| acceptLicense | Accept feature license terms and conditions. The default value is `false`.  | No |
| whenFileExists | Specifies the action to take if a file to be installed already exits. Use `fail` to abort the installation, `ignore` to continue the installation and ignore the file that exists, and `replace` to overwrite the existing file.| No |
| to | Specifies feature installation location. Set to `usr` to install as a user feature. Otherwise, set it to any configured product extension location. The default value is `usr`.| No |

The following example shows what properties must be set up to install the [`mongodb-2.0`](https://developer.ibm.com/wasdev/downloads/#asset/features-com.ibm.websphere.appserver.mongodb-2.0) feature to your server:

```
apply plugin: 'liberty'

liberty {
    wlpDir = "c:/wlp"

    features {
        name = ['mongodb-2.0']
        acceptLicense = true
    } 
}
```
