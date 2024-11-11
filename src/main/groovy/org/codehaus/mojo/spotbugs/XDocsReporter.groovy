/*
 * Copyright 2005-2024 the original author or authors.
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

import edu.umd.cs.findbugs.Version

import groovy.xml.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder

import java.nio.charset.StandardCharsets

import org.apache.maven.plugin.logging.Log

/**
 * The reporter controls the generation of the Spotbugs report.
 */
class XDocsReporter {

    /** The key to get the value if the line number is not available. */
    static final String NOLINE_KEY = 'report.spotbugs.noline'

    /** The bundle to get the messages from. */
    ResourceBundle bundle

    /** The logger to write logs to. */
    Log log

    /** The threshold of bugs severity. */
    String threshold

    /** The used effort for searching bugs. */
    String effort

    /** The output Writer stream. */
    Writer outputWriter

    /** Spotbugs Results. */
    GPathResult spotbugsResults

    /** Bug Classes. */
    List bugClasses

    /** The directories containing the sources to be compiled. */
    List compileSourceRoots

    /** The directories containing the test sources to be compiled. */
    List testSourceRoots

    /** The output encoding. */
    String outputEncoding

    /**
     * Default constructor.
     *
     * @param bundle
     *            The Resource Bundle to use
     */
    XDocsReporter(ResourceBundle bundle, Log log, String threshold, String effort, String outputEncoding) {
        assert bundle
        assert log
        assert threshold
        assert effort
        assert outputEncoding

        this.bundle = bundle
        this.log = log
        this.threshold = threshold
        this.effort = effort
        this.outputEncoding = outputEncoding

        this.outputWriter = null
        this.spotbugsResults = null

        this.compileSourceRoots = []
        this.testSourceRoots = []
        this.bugClasses = []
    }

    /**
     * Returns the threshold string value for the integer input.
     *
     * @param thresholdValue
     *            The ThresholdValue integer to evaluate.
     * @return The string valueof the Threshold object.
     */
    protected String evaluateThresholdParameter(String thresholdValue) {
        String thresholdName

        switch (thresholdValue) {
            case '1':
                thresholdName = 'High'
                break
            case '2':
                thresholdName = 'Normal'
                break
            case '3':
                thresholdName = 'Low'
                break
            case '4':
                thresholdName = 'Exp'
                break
            case '5':
                thresholdName = 'Ignore'
                break
            default:
                thresholdName = 'Invalid Priority'
        }

        return thresholdName
    }

    /**
     * Gets the Spotbugs Version of the report.
     *
     * @return The Spotbugs Version used on the report.
     */
    protected String getSpotBugsVersion() {
        return Version.VERSION_STRING
    }

    public void generateReport() {
        StreamingMarkupBuilder xmlBuilder = new StreamingMarkupBuilder()
        xmlBuilder.encoding = StandardCharsets.UTF_8.name()

        def xdoc = {
            mkp.xmlDeclaration()
            log.debug("generateReport spotbugsResults is ${spotbugsResults}")

            BugCollection(version: getSpotBugsVersion(), threshold: SpotBugsInfo.spotbugsThresholds.get(threshold), effort: SpotBugsInfo.spotbugsEfforts.get(effort)) {

                log.debug("spotbugsResults.FindBugsSummary total_bugs is ${spotbugsResults.FindBugsSummary.@total_bugs.text()}")

                spotbugsResults.FindBugsSummary.PackageStats.ClassStats.each() {classStats ->

                    String classStatsValue = classStats.'@class'.text()
                    String classStatsBugCount = classStats.'@bugs'.text()

                    log.debug('classStats...')
                    log.debug("classStatsValue is ${classStatsValue}")
                    log.debug("classStatsBugCount is ${classStatsBugCount}")

                    if (Integer.parseInt(classStatsBugCount) > 0) {
                        bugClasses << classStatsValue
                    }
                }

                bugClasses.each() { bugClass ->
                    log.debug("finish bugClass is ${bugClass}")
                    file(classname: bugClass) {
                        spotbugsResults.BugInstance.each() { bugInstance ->

                            if (bugInstance.Class.find{ it.@primary == "true" }.@classname.text() != bugClass) {
                                return
                            }

                            String type = bugInstance.@type.text()
                            String category = bugInstance.@category.text()
                            String message = bugInstance.LongMessage.text()
                            String priority = evaluateThresholdParameter(bugInstance.@priority.text())
                            String line = bugInstance.SourceLine.@start[0].text()
                            log.debug("BugInstance message is ${message}")

                            BugInstance(type: type, priority: priority, category: category, message: message, lineNumber: ((line) ? line: "-1"))
                        }
                    }
                }

                log.debug("Printing Errors")

                Error() {
                    spotbugsResults.Error.analysisError.each() {analysisError ->
                        AnalysisError(analysisError.message.text())
                    }

                    log.debug("Printing Missing classes")

                    spotbugsResults.Error.MissingClass.each() {missingClass ->
                        MissingClass(missingClass.text)
                    }
                }

                Project() {
                    log.debug("Printing Source Roots")

                    if (!compileSourceRoots.isEmpty()) {
                        compileSourceRoots.each() { srcDir ->
                            log.debug("SrcDir is ${srcDir}")
                            SrcDir(srcDir)
                        }
                    }

                    if (!testSourceRoots.isEmpty()) {
                        testSourceRoots.each() { srcDir ->
                            log.debug("SrcDir is ${srcDir}")
                            SrcDir(srcDir)
                        }
                    }
                }
            }
        }

        outputWriter << xmlBuilder.bind(xdoc)
        outputWriter.close()
    }
}
