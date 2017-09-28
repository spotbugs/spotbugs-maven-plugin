package org.codehaus.mojo.findbugs

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import groovy.util.slurpersupport.GPathResult
import org.apache.maven.plugin.logging.Log
import groovy.xml.StreamingMarkupBuilder


/**
 * The reporter controls the generation of the Spotbugs report.
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id: XDocsReporter.groovy 15967 2012-02-15 16:18:40Z gleclaire $
 */
class XDocsReporter {

	/**
	 * The key to get the value if the line number is not available.
	 *
	 */
	static final String NOLINE_KEY = "report.spotbugs.noline"

	/**
	 * The bundle to get the messages from.
	 *
	 */
	ResourceBundle bundle

	/**
	 * The logger to write logs to.
	 *
	 */
	Log log

	/**
	 * The threshold of bugs severity.
	 *
	 */
	String threshold

	/**
	 * The used effort for searching bugs.
	 *
	 */
	String effort

	/**
	 * The output Writer stream.
	 *
	 */
	Writer outputWriter

	GPathResult spotbugsResults

	List bugClasses

	/**
	 * The directories containing the sources to be compiled.
	 *
	 */
	List compileSourceRoots

	List testSourceRoots

	String outputEncoding



	/**
	 * Default constructor.
	 *
	 * @param bundle - The Resource Bundle to use
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
	 *
	 */
	protected String evaluateThresholdParameter(String thresholdValue) {
		String thresholdName

		switch ( thresholdValue ) {
			case "1":
				thresholdName = "High"
				break
			case "2":
				thresholdName = "Normal"
				break
			case "3":
				thresholdName = "Low"
				break
			case "4":
				thresholdName = "Exp"
				break
			case "5":
				thresholdName = "Ignore"
				break
			default:
				thresholdName = "Invalid Priority"
		}

		return thresholdName
	}

	/**
	 * Gets the Spotbugs Version of the report.
	 *
	 * @return The Spotbugs Version used on the report.
	 *
	 */
	protected String getFindBugsVersion() {
		return edu.umd.cs.findbugs.Version.VERSION_STRING
	}


	public void generateReport() {

		def xmlBuilder = new StreamingMarkupBuilder()
		xmlBuilder.encoding = "UTF-8"

		def xdoc = {
			mkp.xmlDeclaration()
			log.debug("generateReport spotbugsResults is ${spotbugsResults}")


			BugCollection(version: getFindBugsVersion(), threshold: FindBugsInfo.spotbugsThresholds.get(threshold), effort: FindBugsInfo.spotbugsEfforts.get(effort)) {

				log.debug("spotbugsResults.SpotBugsSummary total_bugs is ${spotbugsResults.SpotBugsSummary.@total_bugs.text()}")

				spotbugsResults.FindBugsSummary.PackageStats.ClassStats.each() {classStats ->

					def classStatsValue = classStats.'@class'.text()
					def classStatsBugCount = classStats.'@bugs'.text()

					log.debug("classStats...")
					log.debug("classStatsValue is ${classStatsValue}")
					log.debug("classStatsBugCount is ${classStatsBugCount}")

					if ( Integer.parseInt(classStatsBugCount) > 0 ) {
						bugClasses << classStatsValue
					}
				}

				bugClasses.each() {bugClass ->
					log.debug("finish bugClass is ${bugClass}")
					file(classname: bugClass) {
						spotbugsResults.BugInstance.each() {bugInstance ->

							if ( bugInstance.Class.find{ it.@primary == "true" }.@classname.text() == bugClass ) {

								def type = bugInstance.@type.text()
								def category = bugInstance.@category.text()
								def message = bugInstance.LongMessage.text()
								def priority = evaluateThresholdParameter(bugInstance.@priority.text())
								def line = bugInstance.SourceLine.@start[0].text()
								log.debug("BugInstance message is ${message}")

								BugInstance(type: type, priority: priority, category: category, message: message, lineNumber: ((line) ? line: "-1"))
							}
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

					if ( !compileSourceRoots.isEmpty() ) {
						compileSourceRoots.each() {srcDir ->
							log.debug("SrcDir is ${srcDir}")
							SrcDir(srcDir)
						}
					}

					if ( !testSourceRoots.isEmpty() ) {
						testSourceRoots.each() {srcDir ->
							log.debug("SrcDir is ${srcDir}")
							SrcDir(srcDir)
						}
					}
				}
			}
		}

		outputWriter << xmlBuilder.bind(xdoc)
		outputWriter.flush()
		outputWriter.close()

	}
}
