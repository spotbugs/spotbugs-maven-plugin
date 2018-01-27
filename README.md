# Spotbugs Maven Plugin

[![Build Status](https://travis-ci.org/spotbugs/spotbugs-maven-plugin.svg?branch=spotbugs)](https://travis-ci.org/spotbugs/spotbugs-maven-plugin)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.spotbugs/spotbugs-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.spotbugs/spotbugs-maven-plugin)
[![Apache 2](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Latest Snapshot ##

Please download latest snapshots from [here](https://oss.sonatype.org/content/repositories/snapshots/com/github/spotbugs/spotbugs-maven-plugin/)

Building spotbugs-maven-plugin Requirements
===========================================

Java 8 is required for spotbugs analysis.

spotbugs-maven-plugin
=====================

Maven Mojo Plug-In to generate reports based on the SpotBugs Analyzer

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
mvn com.github.spotbugs:spotbugs-maven-plugin:3.1.1:gui 
```
