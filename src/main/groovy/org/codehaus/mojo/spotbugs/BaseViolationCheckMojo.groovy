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
        log.debug('Executing spotbugs mojo')

        if (skip) {
            log.info('Spotbugs plugin skipped')
            return
        }

        if (!doSourceFilesExist()) {
            log.debug('Nothing for SpotBugs to do here.')
            return
        }

        log.debug('Files found to process spotbugs')

        Path outputDir = spotbugsXmlOutputDirectory.toPath()

        if (Files.notExists(outputDir)) {
            try {
                Files.createDirectories(outputDir)
            } catch (IOException e) {
                throw new MojoExecutionException('Cannot create xml output directory', e)
            }
        }

        Path outputFile = outputDir.resolve(spotbugsXmlOutputFilename)

        if (Files.notExists(outputFile)) {
            log.debug('Output file does not exist!')
            return
        }

        Node xml = new XmlParser().parse(outputFile.toFile())

        NodeList bugs = xml.BugInstance
        int bugCount = bugs.size()
        int errorCount = xml.Error.size()
        if (log.isInfoEnabled()) {
            log.info("BugInstance size is ${bugCount}")
            log.info("Error size is ${errorCount}")
        }

        if (bugCount <= 0) {
            log.info('No errors/warnings found')
            return
        } else if (maxAllowedViolations > 0 && bugCount <= maxAllowedViolations) {
            if (log.isInfoEnabled()) {
                log.info("Total ${bugCount} violations are found as acceptable using configured property maxAllowedViolations " +
                    ":${maxAllowedViolations}.${SpotBugsInfo.EOL}Below are list of bugs ignored :${SpotBugsInfo.EOL}")
            }
            printBugs(bugs)
            return
        }

        if (log.isInfoEnabled()) {
            log.info('Total bugs: ' + bugCount)
        }

        int priorityThresholdNum = failThreshold ? SpotBugsInfo.spotbugsPriority.indexOf(failThreshold) : Integer.MAX_VALUE
        if (priorityThresholdNum == -1) {
            throw new MojoExecutionException("Invalid value for failThreshold: ${failThreshold}")
        }

        int bugCountAboveThreshold = 0
        bugs.each { Node bug ->
            int priorityNum = bug.'@priority'.toInteger()
            // lower is more severe
            if (priorityNum <= priorityThresholdNum) {
                bugCountAboveThreshold++
            }

            if (!quiet && (log.isErrorEnabled() || log.isInfoEnabled())) {
                String logMsg = SpotBugsInfo.spotbugsPriority[priorityNum] + ': ' + bugLog(bug)

                // lower is more severe
                if (priorityNum <= priorityThresholdNum) {
                    if (log.isErrorEnabled()) {
                        log.error(logMsg)
                    }
                } else if (log.isInfoEnabled()) {
                    log.info(logMsg)
                }
            }
        }

        if (log.isInfoEnabled()) {
            log.info(SpotBugsInfo.EOL + SpotBugsInfo.EOL + SpotBugsInfo.EOL +
                'To see bug detail using the Spotbugs GUI, use the following command "mvn spotbugs:gui"' +
                SpotBugsInfo.EOL + SpotBugsInfo.EOL + SpotBugsInfo.EOL)
        }

        if ((bugCountAboveThreshold || errorCount) && failOnError) {
            throw new MojoExecutionException("failed with ${bugCountAboveThreshold} bugs and ${errorCount} errors")
        }
    }

    private boolean doSourceFilesExist() {
        boolean foundClassFiles = false
        List<String> classFilesList = []
        if (this.classFilesDirectory.isDirectory()) {
            foundClassFiles = walkFiles(classFilesList, classFilesDirectory, foundClassFiles)
        }

        boolean foundTestFiles = false
        List<String> testFilesList = []
        if (this.includeTests && this.testClassFilesDirectory.isDirectory()) {
            foundTestFiles = walkFiles(testFilesList, testClassFilesDirectory, foundTestFiles)
        }

        return foundClassFiles || foundTestFiles
    }

    private boolean walkFiles(List filesList, File filesDirectory, boolean foundFiles) {
        if (log.isDebugEnabled()) {
            log.debug('looking for files with extensions: ' + SpotBugsInfo.EXTENSIONS)
            Files.walk(filesDirectory.toPath()).filter { Path path ->
                SpotBugsInfo.EXTENSIONS.any { String ext ->
                    path.toString().toLowerCase(Locale.getDefault()).endsWith(ext)
                }
            }.forEach { Path path ->
                filesList.add(path.toString())
            }
            foundFiles = !filesList.isEmpty()
        } else {
            foundFiles = Files.walk(filesDirectory.toPath()).anyMatch { Path path ->
                SpotBugsInfo.EXTENSIONS.any { String ext ->
                    path.toString().toLowerCase(Locale.getDefault()).endsWith(ext)
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("SourceFiles found flag: ${foundFiles} with count: ${filesList.size()}")
            if (!filesList.isEmpty()) {
                log.debug("Files found: " + filesList)
            }
        }

        return foundFiles
    }

    private void printBugs(NodeList bugs) {
        if (log.isErrorEnabled()) {
            StringBuilder sb = new StringBuilder()
            bugs.each { Node bug ->
                sb.append(bugLog(bug)).append(SpotBugsInfo.EOF)
            }
            log.error(sb.toString())
        }
    }

    // Protected to allow groovy closure to see this method
    protected static String bugLog(Node bug) {
        return bug.LongMessage.text() + SpotBugsInfo.BLANK + bug.SourceLine.'@classname' + SpotBugsInfo.BLANK +
            bug.SourceLine.Message.text() + SpotBugsInfo.BLANK + bug.'@type'
    }

}
