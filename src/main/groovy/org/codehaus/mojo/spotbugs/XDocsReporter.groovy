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

import edu.umd.cs.findbugs.Version

import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChild
import groovy.xml.StreamingMarkupBuilder
import java.nio.charset.Charset

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
    List<String> bugClasses = []

    /** The directories containing the sources to be compiled. */
    List<String> compileSourceRoots = []

    /** The directories containing the test sources to be compiled. */
    List<String> testSourceRoots = []

    /** The output encoding. */
    Charset outputEncoding

    /**
     * Default constructor.
     *
     * @param bundle
     *            The Resource Bundle to use
     */
    XDocsReporter(ResourceBundle bundle, Log log, String threshold, String effort, String outputEncoding) {
        if (!bundle || !log || !threshold || !effort || !outputEncoding) {
            throw new IllegalArgumentException('All constructor arguments must be provided')
        }

        this.bundle = bundle
        this.log = log
        this.threshold = threshold
        this.effort = effort
        this.outputEncoding = Charset.forName(outputEncoding)
    }

    /**
     * Returns the threshold string value for the integer input.
     *
     * @param thresholdValue
     *            The ThresholdValue integer to evaluate.
     * @return The string valueof the Threshold object.
     */
    protected static String evaluateThresholdParameter(String thresholdValue) {
        switch (thresholdValue) {
            case '1': return 'High'
            case '2': return 'Normal'
            case '3': return 'Low'
            case '4': return 'Exp'
            case '5': return 'Ignore'
            default: return 'Invalid Priority'
        }
    }

    /**
     * Gets the Spotbugs Version of the report.
     *
     * @return The Spotbugs Version used on the report.
     */
    protected static String getSpotBugsVersion() {
        return Version.VERSION_STRING
    }

    public void generateReport() {
        StreamingMarkupBuilder xmlBuilder = new StreamingMarkupBuilder()
        xmlBuilder.encoding = outputEncoding.name()

        outputWriter << xmlBuilder.bind {
            mkp.xmlDeclaration()
            if (log.isDebugEnabled()) {
                log.debug("generateReport spotbugsResults is ${spotbugsResults}")
            }

            BugCollection(version: getSpotBugsVersion(), threshold: SpotBugsInfo.spotbugsThresholds[threshold],
                    effort: SpotBugsInfo.spotbugsEfforts[effort]) {

                if (log.isDebugEnabled()) {
                    log.debug("spotbugsResults.FindBugsSummary total_bugs is ${spotbugsResults.FindBugsSummary.@total_bugs.text()}")
                }

                spotbugsResults.FindBugsSummary.PackageStats.ClassStats.each() { NodeChild classStats ->

                    String classStatsValue = classStats.'@class'.text()
                    String classStatsBugCount = classStats.'@bugs'.text()

                    if (log.isDebugEnabled()) {
                        log.debug('classStats...')
                        log.debug("classStatsValue is ${classStatsValue}")
                        log.debug("classStatsBugCount is ${classStatsBugCount}")
                    }

                    if (Integer.parseInt(classStatsBugCount) > 0) {
                        bugClasses << classStatsValue
                    }
                }

                bugClasses.each() { String bugClass ->
                    if (log.isDebugEnabled()) {
                        log.debug("finish bugClass is ${bugClass}")
                    }
                    file(classname: bugClass) {
                        spotbugsResults.BugInstance.each() { NodeChild bugInstance ->

                            if (bugInstance.Class.find { NodeChild classNode -> classNode.@primary == "true" }.@classname.text() != bugClass) {
                                return
                            }

                            String type = bugInstance.@type.text()
                            String category = bugInstance.@category.text()
                            String message = bugInstance.LongMessage.text()
                            String priority = evaluateThresholdParameter(bugInstance.@priority.text())
                            String line = bugInstance.SourceLine.@start[0].text()
                            if (log.isDebugEnabled()) {
                                log.debug("BugInstance message is ${message}")
                            }

                            BugInstance(type: type, priority: priority, category: category, message: message,
                                    lineNumber: ((line) ? line: "-1"))
                        }
                    }
                }

                log.debug("Printing Errors")

                Error() {
                    spotbugsResults.Error.analysisError.each() { NodeChild analysisError ->
                        AnalysisError(analysisError.message.text())
                    }

                    log.debug("Printing Missing classes")

                    spotbugsResults.Error.MissingClass.each() { NodeChild missingClass ->
                        MissingClass(missingClass.text)
                    }
                }

                Project() {
                    log.debug("Printing Source Roots")

                    if (!compileSourceRoots.isEmpty()) {
                        compileSourceRoots.each() { String srcDir ->
                            if (log.isDebugEnabled()) {
                                log.debug("SrcDir is ${srcDir}")
                            }
                            SrcDir(srcDir)
                        }
                    }

                    if (!testSourceRoots.isEmpty()) {
                        testSourceRoots.each() { String srcDir ->
                            if (log.isDebugEnabled()) {
                                log.debug("SrcDir is ${srcDir}")
                            }
                            SrcDir(srcDir)
                        }
                    }
                }
            }
        }

        outputWriter.close()
    }
}
