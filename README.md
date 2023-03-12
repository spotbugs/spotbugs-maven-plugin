# Spotbugs Maven Plugin

[![Java CI](https://github.com/spotbugs/spotbugs-maven-plugin/workflows/Java%20CI/badge.svg)](https://github.com/spotbugs/spotbugs-maven-plugin/actions?query=workflow%3A%22Java+CI%22)
[![Java Integration Tests](https://github.com/spotbugs/spotbugs-maven-plugin/workflows/Java%20Integration%20Tests/badge.svg)](https://github.com/spotbugs/spotbugs-maven-plugin/actions?query=workflow%3A%22Java+Integration+Tests%22)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.spotbugs/spotbugs-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.spotbugs/spotbugs-maven-plugin)
[![Apache 2](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Latest Snapshot ##

Please download latest snapshots from [here](https://oss.sonatype.org/content/repositories/snapshots/com/github/spotbugs/spotbugs-maven-plugin/)

Building spotbugs-maven-plugin Requirements
===========================================

Java 8+ is required for spotbugs analysis.

spotbugs-maven-plugin
=====================

Maven Mojo Plug-In to generate reports based on the SpotBugs Analyzer

See site page for [usage](https://spotbugs.github.io/spotbugs-maven-plugin/)

## Special notice ##

Continue to use 'FindBugsFilter' when needed as the spotbugs project has not yet renamed that to reflect project.

## Usage ##

The [SpotBugs documentation](https://spotbugs.readthedocs.io/en/latest/maven.html) describes the pom.xml modifications and Maven goals.

## Running Tests ##

Run all tests
```
mvn -DtestSrc=remote -Prun-its clean install -D"invoker.parallelThreads=4"
```
Skip tests
```
mvn -DskipTests=true clean install
```
Run tests on spotbugs test source code that is local instead of from SpotBugs github repository
```
mvn -DtestSrc=local -DlocalTestSrc=/opt/spotBugs -Prun-its clean install -D"invoker.parallelThreads=4"
```

Run selected tests
```
mvn -DtestSrc=remote -Prun-its -Dinvoker.test=build-*,basic-1,check-nofail clean install -D"invoker.parallelThreads=4"
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
mvn com.github.spotbugs:spotbugs-maven-plugin:4.0.0:gui
```

## Contributing ##

Run integration tests
```
mvn clean install -P run-its -DtestSrc=remote
```

## Groovy ##

This plugin is written entirey in groovy.  It does have limitations when it comes to groovy in relation to java releases.  Every attempt is made to ensure fast releases to pick up groovy changes related to java.

Known issues

The security manager is turned off by default in jdk 18/19 and scheduled from removal in a future java release, therefore to use this plugin with jdk 18/19, the security manager may need turned back on using ```JAVA_OPTS``` to ```-Djava.security.manager=allow```.  See [groovy](https://groovy-lang.org/releasenotes/groovy-4.0.html) for more details.

If using groovy with same group id (```org.codehaus.groovy 3.x``` or before or ```org.apache.groovy 4.x or above```), an error may occur if not on same version. To alleviate that, make sure groovy artifacts are defined in ```dependency management``` in order to force the loaded version correctly on your usage.


## Eclipse m2e Integration ##

The plugin cycles controlled by Eclipse require compilation phase for m2e without further help.  This plugin runs verify and during site generation.  Therefore Eclipse m2e will show up but not do anything with this plugin alone.  In order to have proper execution within Ecipse m2e, use [m2e-code-quality](https://github.com/m2e-code-quality/m2e-code-quality) plugin for spotbugs.
