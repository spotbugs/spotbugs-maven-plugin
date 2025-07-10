/*
 * Copyright 2005-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.mojo.spotbugs

import groovy.xml.XmlParser

import java.nio.file.Files
import java.nio.file.Path

import org.apache.commons.io.FileUtils
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Parameter

abstract class BaseViolationCheckMojo extends AbstractMojo {

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
    @Parameter(defaultValue = 'spotbugsXml.xml', property = 'spotbugs.outputXmlFilename')
    String spotbugsXmlOutputFilename

    /** Directory containing the class files for Spotbugs to analyze. */
    @Parameter(defaultValue = '${project.build.outputDirectory}', required = true)
    File classFilesDirectory

    /** Directory containing the test class files for Spotbugs to analyze. */
    @Parameter(defaultValue = '${project.build.testOutputDirectory}', required = true)
    File testClassFilesDirectory

    /**
     * Run Spotbugs on the tests.
     *
     * @since 2.0
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.includeTests')
    boolean includeTests

    /** Turn on Spotbugs debugging. */
    @Parameter(defaultValue = 'false', property = 'spotbugs.debug')
    boolean debug

    /**
     * Skip entire check.
     *
     * @since 1.1
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.skip')
    boolean skip

    /**
     * Fail the build on an error.
     *
     * @since 2.0
     */
    @Parameter(defaultValue = 'true', property = 'spotbugs.failOnError')
    boolean failOnError

    /**
     * Priority threshold which bugs have to reach to cause a failure. Valid values are High, Medium or Low.
     * Bugs below this threshold will just issue a warning log entry.
     *
     * @since 4.0.1
     */
    @Parameter(property = 'spotbugs.failThreshold')
    String failThreshold

    /**
     * specified max number of violations which can be ignored by the spotbugs.
     *
     * @since 2.4.1
     */
    @Parameter(defaultValue = '0', property = 'spotbugs.maxAllowedViolations')
    int maxAllowedViolations

    /** Disable bugs log. */
    @Parameter(defaultValue = 'false', property = 'spotbugs.quiet')
    boolean quiet

    @Override
    void execute() {
        log.debug('Executing spotbugs:check')

        if (skip) {
            log.info('Spotbugs plugin skipped')
            return
        }

        if (!doSourceFilesExist()) {
            log.debug('Nothing for SpotBugs to do here.')
            return
        }

        log.debug('Executing spotbugs:check')

        Path outputDir = spotbugsXmlOutputDirectory.toPath()

        if (Files.notExists(outputDir) && !Files.createDirectories(outputDir)) {
            throw new MojoExecutionException('Cannot create xml output directory')
        }

        Path outputFile = outputDir.resolve(spotbugsXmlOutputFilename)

        if (Files.notExists(outputFile)) {
            log.debug('Output file does not exist!')
            return
        }

        Node xml = new XmlParser().parse(outputFile.toFile())

        NodeList bugs = xml.BugInstance
        int bugCount = bugs.size()
        log.info("BugInstance size is ${bugCount}")

        int errorCount = xml.Error.size()
        log.info("Error size is ${errorCount}")

        if (bugCount <= 0) {
            log.info('No errors/warnings found')
            return
        } else if (maxAllowedViolations > 0 && bugCount <= maxAllowedViolations) {
            log.info("Total ${bugCount} violations are found as acceptable using configured property maxAllowedViolations " +
                ":${maxAllowedViolations}.${SpotBugsInfo.EOL}Below are list of bugs ignored :${SpotBugsInfo.EOL}")
            printBugs(bugs)
            return
        }

        log.info('Total bugs: ' + bugCount)

        int priorityThresholdNum = failThreshold ? SpotBugsInfo.spotbugsPriority.indexOf(failThreshold) : Integer.MAX_VALUE
        if (priorityThresholdNum == -1) {
            throw new MojoExecutionException("Invalid value for failThreshold: ${failThreshold}")
        }

        int bugCountAboveThreshold = 0
        bugs.each { Node bug ->
            int priorityNum = bug.'@priority' as Integer
            String priorityName = SpotBugsInfo.spotbugsPriority[priorityNum]
            String logMsg = priorityName + ': ' + bug.LongMessage.text() + SpotBugsInfo.BLANK +
                bug.SourceLine.'@classname' + SpotBugsInfo.BLANK + bug.SourceLine.Message.text() +
                SpotBugsInfo.BLANK + bug.'@type'

            // lower is more severe
            if (priorityNum <= priorityThresholdNum) {
                bugCountAboveThreshold++
                if (!quiet) {
                    log.error(logMsg)
                }
            } else if (!quiet) {
                log.info(logMsg)
            }
        }

        log.info(SpotBugsInfo.EOL + SpotBugsInfo.EOL + SpotBugsInfo.EOL +
            'To see bug detail using the Spotbugs GUI, use the following command "mvn spotbugs:gui"' +
            SpotBugsInfo.EOL + SpotBugsInfo.EOL + SpotBugsInfo.EOL)

        if ((bugCountAboveThreshold || errorCount) && failOnError) {
            throw new MojoExecutionException("failed with ${bugCountAboveThreshold} bugs and ${errorCount} errors")
        }
    }

    private boolean doSourceFilesExist() {
        Collection<File> sourceFiles = []

        if (this.classFilesDirectory.isDirectory()) {
            log.debug('looking for class files with extensions: ' + SpotBugsInfo.EXTENSIONS)
            sourceFiles.addAll(FileUtils.listFiles(classFilesDirectory, SpotBugsInfo.EXTENSIONS, true))
        }

        if (this.includeTests && this.testClassFilesDirectory.isDirectory()) {
            log.debug('looking for test class files: ' + SpotBugsInfo.EXTENSIONS)
            sourceFiles.addAll(FileUtils.listFiles(testClassFilesDirectory, SpotBugsInfo.EXTENSIONS, true))
        }

        log.debug("SourceFiles: ${sourceFiles}")
        !sourceFiles.isEmpty()
    }

    private void printBugs(NodeList bugs) {
        bugs.forEach{ Node bug ->
            log.error(bug.LongMessage.text() + SpotBugsInfo.BLANK + bug.SourceLine.'@classname' + SpotBugsInfo.BLANK +
                bug.SourceLine.Message.text() + SpotBugsInfo.BLANK + bug.'@type')
        }
    }
}
