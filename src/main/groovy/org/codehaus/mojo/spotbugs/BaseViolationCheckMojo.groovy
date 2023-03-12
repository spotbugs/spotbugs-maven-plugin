package org.codehaus.mojo.spotbugs

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import groovy.xml.XmlParser
import groovy.xml.XmlSlurper

import org.apache.maven.artifact.repository.ArtifactRepository

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.tools.SiteTool

import org.apache.maven.execution.MavenSession

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException

import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Execute
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope

import org.apache.maven.project.MavenProject

import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver

import org.codehaus.plexus.resource.ResourceManager
import org.codehaus.plexus.util.FileUtils

abstract class BaseViolationCheckMojo extends AbstractMojo {

    /**
     * Location where generated html will be created.
     *
     */
    @Parameter(defaultValue = '${project.reporting.outputDirectory}', required = true)
    File outputDirectory

    /**
     * Turn on and off xml output of the Spotbugs report.
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "false", property = "spotbugs.xmlOutput", required = true)
    boolean xmlOutput

    /**
     * Specifies the directory where the xml output will be generated.
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = '${project.build.directory}', required = true)
    File xmlOutputDirectory

    /**
     * This has been deprecated and is on by default.
     *
     * @since 1.2.0
     *
     */
    @Deprecated
    @Parameter(defaultValue = "true")
    boolean spotbugsXmlOutput

    /**
     * Specifies the directory where the Spotbugs native xml output will be generated.
     *
     * @since 1.2.0
     */
    @Parameter(defaultValue = '${project.build.directory}', required = true)
    File spotbugsXmlOutputDirectory

    /**
     * Set the name of the output XML file produced
     *
     * @since 3.1.12.2
     */
    @Parameter(property = "spotbugs.outputXmlFilename", defaultValue = "spotbugsXml.xml")
    String spotbugsXmlOutputFilename

    /**
     * Doxia Site Renderer.
     */
    @Component(role = Renderer.class)
    Renderer siteRenderer

    /**
     * Directory containing the class files for Spotbugs to analyze.
     */
    @Parameter(defaultValue = '${project.build.outputDirectory}', required = true)
    File classFilesDirectory

    /**
     * Directory containing the test class files for Spotbugs to analyze.
     *
     */
    @Parameter(defaultValue = '${project.build.testOutputDirectory}', required = true)
    File testClassFilesDirectory

    /**
     * Location of the Xrefs to link to.
     *
     */
    @Parameter(defaultValue = '${project.reporting.outputDirectory}/xref')
    File xrefLocation

    /**
     * Location of the Test Xrefs to link to.
     *
     */
    @Parameter(defaultValue = '${project.reporting.outputDirectory}/xref-test')
    File xrefTestLocation

    /**
     * The directories containing the sources to be compiled.
     *
     */
    @Parameter(defaultValue = '${project.compileSourceRoots}', required = true, readonly = true)
    List compileSourceRoots

    /**
     * The directories containing the test-sources to be compiled.
     *
     * @since 2.0
     */
    @Parameter(defaultValue = '${project.testCompileSourceRoots}', required = true, readonly = true)
    List testSourceRoots

    /**
     * Run Spotbugs on the tests.
     *
     * @since 2.0
     */
    @Parameter(defaultValue = "false", property = "spotbugs.includeTests")
    boolean includeTests

    /**
     * List of artifacts this plugin depends on. Used for resolving the Spotbugs core plugin.
     *
     */
    @Parameter(property = "plugin.artifacts", required = true, readonly = true)
    List pluginArtifacts

    /**
     * The local repository, needed to download the coreplugin jar.
     *
     */
    @Parameter(property = "localRepository", required = true, readonly = true)
    ArtifactRepository localRepository

    /**
     * Remote repositories which will be searched for the coreplugin jar.
     *
     */
    @Parameter(property = "project.remoteArtifactRepositories", required = true, readonly = true)
    List remoteArtifactRepositories

    /**
     * Maven Session.
     */
    @Parameter (defaultValue = '${session}', required = true, readonly = true)
    MavenSession session;

    /**
     * Maven Project.
     *
     */
    @Parameter(property = "project", required = true, readonly = true)
    MavenProject project

    /**
     * Encoding used for xml files. Default value is UTF-8.
     *
     */
    @Parameter(defaultValue = "UTF-8", readonly = true)
    String xmlEncoding

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used.
     *
     * @since 2.2
     */
    @Parameter(defaultValue = '${project.build.sourceEncoding}', property = "encoding")
    String sourceEncoding

    /**
     * The file encoding to use when creating the HTML reports. If the property <code>project.reporting.outputEncoding</code>
     * is not set, the platform default encoding is used.
     *
     * @since 2.2
     */
    @Parameter(defaultValue = '${project.reporting.outputEncoding}', property = "outputEncoding")
    String outputEncoding

    /**
     * Threshold of minimum bug severity to report. Valid values are High, Default, Low, Ignore, and Exp (for experimental).
     *
     */
    @Parameter(defaultValue = "Default", property = "spotbugs.threshold")
    String threshold

    /**
     * Artifact resolver, needed to download the coreplugin jar.
     */
    @Component(role = ArtifactResolver.class)
    ArtifactResolver artifactResolver

    /**
     * <p>
     * File name of the include filter. Only bugs in matching the filters are reported.
     * </p>
     *
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     * It supports multiple files separated by a comma
     * </p>
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = "spotbugs.includeFilterFile")
    String includeFilterFile

    /**
     * <p>
     * File name of the exclude filter. Bugs matching the filters are not reported.
     * </p>
     *
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     * It supports multiple files separated by a comma
     * </p>
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = "spotbugs.excludeFilterFile")
    String excludeFilterFile

    /**
     * <p>
     * File names of the baseline files. Bugs found in the baseline files won't be reported.
     * </p>
     *
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     * </p>
     *
     * This is a comma-delimited list.
     *
     * @since 2.4.1
     */
    @Parameter(property = "spotbugs.excludeBugsFile")
    String excludeBugsFile

    /**
     * Effort of the bug finders. Valid values are Min, Default and Max.
     *
     * @since 1.0-beta-1
     */
    @Parameter(defaultValue = "Default", property = "spotbugs.effort")
    String effort

    /**
     * Turn on Spotbugs debugging.
     *
     */
    @Parameter(defaultValue = "false", property = "spotbugs.debug")
    Boolean debug

    /**
     * Relaxed reporting mode. For many detectors, this option suppresses the heuristics used to avoid reporting false
     * positives.
     *
     * @since 1.1
     */
    @Parameter(defaultValue = "false", property = "spotbugs.relaxed")
    Boolean relaxed

    /**
     * The visitor list to run. This is a comma-delimited list.
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = "spotbugs.visitors")
    String visitors

    /**
     * The visitor list to omit. This is a comma-delimited list.
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = "spotbugs.omitVisitors")
    String omitVisitors

    /**
     * <p>
     * The plugin list to include in the report. This is a comma-delimited list.
     * </p>
     *
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a plugin file.
     * </p>
     *
     * @since 1.0-beta-1
     */
    @Parameter( property="spotbugs.pluginList" )
    String pluginList

    /**
     * Restrict analysis to the given comma-separated list of classes and packages.
     *
     * @since 1.1
     */
    @Parameter(property = "spotbugs.onlyAnalyze")
    String onlyAnalyze

    /**
     * This option enables or disables scanning of nested jar and zip files found
     * in the list of files and directories to be analyzed.
     *
     * @since 2.3.2
     */
    @Parameter(property = "spotbugs.nested", defaultValue = "false")
    Boolean nested

    /**
     * Prints a trace of detectors run and classes analyzed to standard output.
     * Useful for troubleshooting unexpected analysis failures.
     *
     * @since 2.3.2
     */
    @Parameter(property = "spotbugs.trace", defaultValue = "false")
    Boolean trace

    /**
     * Maximum bug ranking to record.
     *
     * @since 2.4.1
     */
    @Parameter(property = "spotbugs.maxRank")
    int maxRank

    /**
     * Skip entire check.
     *
     * @since 1.1
     */
    @Parameter(property = "spotbugs.skip", defaultValue = "false")
    boolean skip

    /**
     * Resource Manager.
     *
     * @since 2.0
     */
    @Component(role = ResourceManager.class)
    ResourceManager resourceManager

    /**
     * SiteTool.
     *
     * @since 2.1-SNAPSHOT
     */
    @Component(role = SiteTool.class)
    SiteTool siteTool

    /**
     * Fail the build on an error.
     *
     * @since 2.0
     */
    @Parameter(property = "spotbugs.failOnError", defaultValue = "true")
    boolean failOnError

    /**
     * Prioritiy threshold which bugs have to reach to cause a failure. Valid values are High, Medium or Low.
     * Bugs below this threshold will just issue a warning log entry.
     *
     * @since 4.0.1
     */
    @Parameter(property = "spotbugs.failThreshold")
    String failThreshold

    /**
     * Fork a VM for Spotbugs analysis.  This will allow you to set timeouts and heap size.
     *
     * @since 2.3.2
     */
    @Parameter(property = "spotbugs.fork", defaultValue = "true")
    boolean fork

    /**
     * Maximum Java heap size in megabytes  (default=512).
     * This only works if the <b>fork</b> parameter is set <b>true</b>.
     *
     * @since 2.2
     */
    @Parameter(property = "spotbugs.maxHeap", defaultValue = "512")
    int maxHeap

    /**
     * Specifies the amount of time, in milliseconds, that Spotbugs may run before
     * it is assumed to be hung and is terminated.
     * The default is 600,000 milliseconds, which is ten minutes.
     * This only works if the <b>fork</b> parameter is set <b>true</b>.
     *
     * @since 2.2
     */
    @Parameter(property = "spotbugs.timeout", defaultValue = "600000")
    int timeout

    /**
     * <p>
     * The arguments to pass to the forked VM (ignored if fork is disabled).
     * </p>
     *
     * @since 2.4.1
     */
    @Parameter(property = "spotbugs.jvmArgs")
    String jvmArgs

    /**
     * <p>
     * specified max number of violations which can be ignored by the spotbugs.
     * </p>
     *
     * @since 2.4.1
     */
    @Parameter(property = "spotbugs.maxAllowedViolations", defaultValue = "0")
    int maxAllowedViolations

    @Override
    void execute() {
        log.debug("Executing spotbugs:check")

        if (skip || !doSourceFilesExist()) {
            log.debug("Nothing for SpotBugs to do here.")
            return
        }

        log.debug("Here goes...............Executing spotbugs:check")

        if (!spotbugsXmlOutputDirectory.exists() && !spotbugsXmlOutputDirectory.mkdirs()) {
            throw new MojoExecutionException("Cannot create xml output directory")
        }

        File outputFile = new File("${spotbugsXmlOutputDirectory}/${spotbugsXmlOutputFilename}")

        if (outputFile.exists()) {

            def xml = new XmlParser().parse(outputFile)

            def bugs = xml.BugInstance
            def bugCount = bugs.size()
            log.info("BugInstance size is ${bugCount}")

            def errorCount = xml.Error.size()
            log.info("Error size is ${errorCount}")

            if (bugCount <= 0) {
                log.info('No errors/warnings found')
                return
            } else if (maxAllowedViolations > 0 && bugCount <= maxAllowedViolations) {
                log.info("total ${bugCount} violations are found which is set to be acceptable using configured property maxAllowedViolations :" + maxAllowedViolations + "." + SpotBugsInfo.EOL + "Below are list of bugs ignored :" + SpotBugsInfo.EOL)
                printBugs(bugCount, bugs)
                return;
            }

            log.info('Total bugs: ' + bugCount)
            def bugCountAboveThreshold = 0
            def priorityThresholdNum = failThreshold ? SpotBugsInfo.spotbugsPriority.indexOf(failThreshold) : Integer.MAX_VALUE
            if (priorityThresholdNum == -1) {
                throw new MojoExecutionException("Invalid value for failThreshold: ${failThreshold}")
            }

            for (i in 0..bugCount-1) {
                def bug = bugs[i]
                def priorityNum = bug.'@priority' as Integer
                def priorityName = SpotBugsInfo.spotbugsPriority[priorityNum]
                def logMsg = priorityName + ': ' + bug.LongMessage.text() + SpotBugsInfo.BLANK + bug.SourceLine.'@classname' + SpotBugsInfo.BLANK +
                        bug.SourceLine.Message.text() + SpotBugsInfo.BLANK + bug.'@type'

                // lower is more severe
                if (priorityNum <= priorityThresholdNum) {
                    bugCountAboveThreshold += 1
                    log.error(logMsg)
                } else {
                    log.info(logMsg)
                }
            }

            log.info(SpotBugsInfo.EOL + SpotBugsInfo.EOL + SpotBugsInfo.EOL + 'To see bug detail using the Spotbugs GUI, use the following command "mvn spotbugs:gui"' + SpotBugsInfo.EOL + SpotBugsInfo.EOL + SpotBugsInfo.EOL)

            if ((bugCountAboveThreshold || errorCount) && failOnError) {
                throw new MojoExecutionException("failed with ${bugCountAboveThreshold} bugs and ${errorCount} errors ")
            }
        }
    }

    private boolean doSourceFilesExist() {
        List sourceFiles = new ArrayList()

        if (this.classFilesDirectory.exists() && this.classFilesDirectory.isDirectory()) {
            sourceFiles.addAll(FileUtils.getFiles(classFilesDirectory, SpotBugsInfo.JAVA_REGEX_PATTERN, null))
        }

        if (this.includeTests && this.testClassFilesDirectory.exists() && this.testClassFilesDirectory.isDirectory()) {
            sourceFiles.addAll(FileUtils.getFiles(testClassFilesDirectory, SpotBugsInfo.JAVA_REGEX_PATTERN, null))
        }

        !sourceFiles.isEmpty()
    }

    private void printBugs(total, bugs) {
        for (i in 0..total - 1) {
            def bug = bugs[i]
            log.error( bug.LongMessage.text() + SpotBugsInfo.BLANK + bug.SourceLine.'@classname' + SpotBugsInfo.BLANK + bug.SourceLine.Message.text() + SpotBugsInfo.BLANK + bug.'@type')
        }
    }

}
