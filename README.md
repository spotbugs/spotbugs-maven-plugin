# Spotbugs Maven Plugin

[![Java CI](https://github.com/spotbugs/spotbugs-maven-plugin/workflows/Java%20CI/badge.svg)](https://github.com/spotbugs/spotbugs-maven-plugin/actions?query=workflow%3A%22Java+CI%22)
[![Java Integration Tests](https://github.com/spotbugs/spotbugs-maven-plugin/workflows/Java%20Integration%20Tests/badge.svg)](https://github.com/spotbugs/spotbugs-maven-plugin/actions?query=workflow%3A%22Java+Integration+Tests%22)
[![Coverage Status](https://coveralls.io/repos/github/spotbugs/spotbugs-maven-plugin/badge.svg?branch=master)](https://coveralls.io/github/spotbugs/spotbugs-maven-plugin?branch=master)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=spotbugs_spotbugs-maven-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=spotbugs_spotbugs-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.spotbugs/spotbugs-maven-plugin.svg)](https://search.maven.org/com.github.spotbugs/spotbugs-maven-plugin)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/com/github/spotbugs/spotbugs-maven-plugin/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/com/github/spotbugs/spotbugs-maven-plugin/README.md)
[![Apache 2](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Latest Snapshot ##

Please download latest snapshots from [here](https://oss.sonatype.org/content/repositories/snapshots/com/github/spotbugs/spotbugs-maven-plugin/)

### Building spotbugs-maven-plugin Requirements ###

* Java 17+ is required to build the project.
* Maven 3.9.11 is required to build the project.

### Running spotbugs-maven-plugin Requirements ###

* Java 11 or better is required for spotbugs analysis.
* Maven 3.6.3 or better is required for usage.

### spotbugs-maven-plugin ###

Maven Mojo Plug-In to generate reports based on the SpotBugs Analyzer

See site page for [usage](https://spotbugs.github.io/spotbugs-maven-plugin/)

## Special notice ##

Continue to use `FindBugsFilter` when needed as the spotbugs project has not yet renamed that to reflect project.

## Usage ##

The [SpotBugs documentation](https://spotbugs.readthedocs.io/en/latest/maven.html) describes the pom.xml modifications and Maven goals.

## Running Tests ##

Run all tests
```
mvn -DtestSrc=remote -Prun-its clean install -D"invoker.parallelThreads=8"
```
Skip tests
```
mvn -DskipTests=true clean install
```
Run tests on spotbugs test source code that is local instead of from SpotBugs github repository
```
mvn -DtestSrc=local -DlocalTestSrc=/opt/spotBugs -Prun-its clean install -D"invoker.parallelThreads=8"
```

Run selected tests
```
mvn -DtestSrc=remote -Prun-its -Dinvoker.test=build-*,basic-1,check-nofail clean install -D"invoker.parallelThreads=8"
```

Run tests in debugger
```
mvn -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE" -Prun-its clean install
```

Run selected tests in debugger
```
mvn -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE" -Prun-its -Dinvoker.test=build-*,basic-1,check clean install
```

Run gui with a specific version 
```
mvn com.github.spotbugs:spotbugs-maven-plugin:${spotbugs.plugin}:gui
```

## Creating new site examples ##

This product `site' contains a copy of an integration test result using spotbugs to show how it works.  To update that, use the following instructions.

* Execute `mvn -DtestSrc=remote -Prun-its clean install -D"invoker.parallelThreads=8"`
* Then replace current `src/site/resources` entirely with any working example 'site' folder from there (Currently uses `basic-1`) and include the top files top level of the basic site folder `spotbugs.xml` and `spotbugsXml.xml`.
* Commit results and submit a pull request to apply.

## Contributing ##

Run integration tests
```
mvn clean install -P run-its -DtestSrc=remote
```

## Groovy ##

This plugin is written entirely in Groovy.  It does have limitations when it comes to Groovy in relation to java releases.  Every attempt is made to ensure fast releases to pick up Groovy changes related to java.

Known issues

The security manager is turned off by default in jdk 18 and scheduled from removal in a future java release, therefore to use this plugin with jdk 18+,
the security manager may need to be turned back on by setting `JAVA_OPTS` to `-Djava.security.manager=allow`.
See [groovy](https://groovy-lang.org/releasenotes/groovy-4.0.html) for more details.

If using Groovy with same group id (`org.codehaus.groovy 3.x` and before; or `org.apache.groovy 4.x and above`),
an error may occur if not on same version. To alleviate that, make sure Groovy artifacts are defined in `dependencyManagement`
to ensure the correct version is loaded.


## Eclipse m2e Integration ##

The plugin cycles controlled by Eclipse require compilation phase for m2e without further help.  This plugin runs verify only during site generation.
Therefore Eclipse m2e will show up but not do anything with this plugin alone.  In order to have proper execution within Eclipse m2e,
use [m2e-code-quality](https://github.com/m2e-code-quality/m2e-code-quality) plugin for spotbugs.

## Analysis Properties ##

Is there some way to set the [Analysis Properties](https://spotbugs.readthedocs.io/en/stable/analysisprops.html) when using the maven plugin?

Analysis properties are passed as Java system properties, so they can be set in the <jvmArgs> configuration element.

E.g. to set the findbugs.assertionmethods analyzer property:

```
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <configuration>
        <jvmArgs>-Dfindbugs.assertionmethods=org.apache.commons.lang3.Validate.notNull</jvmArgs>
    </configuration>
</plugin>
```

## Override Spotbugs Version ##

Spotbugs aligns with spotbugs releases but normally does not have a hard requirement on this, therefore you can override the spotbugs version as follows.

```
    <plugin>
        <groupId>com.github.spotbugs</groupId>
        <artifactId>spotbugs-maven-plugin</artifactId>
        <version>${spotbugs.plugin}</version>
        <dependencies>
            <dependency>
                <groupId>com.github.spotbugs</groupId>
                <artifactId>spotbugs</artifactId>
                <version><!-- latest version --></version>
            </dependency>
        </dependencies>
    </plugin>
```
