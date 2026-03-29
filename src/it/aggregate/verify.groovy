/*
 * Copyright 2005-2026 the original author or authors.
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
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChild

import java.nio.file.Files
import java.nio.file.Path

println '**********************************'
println 'Checking Individual Module Results'
println '**********************************'

//  Verify module-1 individual SpotBugs XML was generated
Path module1SpotbugsXml = basedir.toPath().resolve("modules/module-1/target/spotbugsXml.xml")
assert Files.exists(module1SpotbugsXml), "Module-1 spotbugsXml.xml should exist"

//  Verify module-2 individual SpotBugs XML was generated
Path module2SpotbugsXml = basedir.toPath().resolve("modules/module-2/target/spotbugsXml.xml")
assert Files.exists(module2SpotbugsXml), "Module-2 spotbugsXml.xml should exist"

XmlSlurper xmlSlurper = new XmlSlurper()
xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

println '************************************'
println 'Checking Aggregate SpotBugs XML file'
println '************************************'

//  Verify the aggregate SpotBugs XML was generated at the root project level
Path aggregateSpotbugsXml = basedir.toPath().resolve("target/spotbugsXml.xml")
assert Files.exists(aggregateSpotbugsXml), "Aggregate spotbugsXml.xml should exist at root project level"

GPathResult aggregateResult = xmlSlurper.parse(aggregateSpotbugsXml)

List<NodeChild> allNodes = aggregateResult.depthFirst().toList()
int aggregateBugCount = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
println "Aggregate BugInstance count: ${aggregateBugCount}"

// Count bugs in each module
GPathResult module1Result = xmlSlurper.parse(module1SpotbugsXml)
GPathResult module2Result = xmlSlurper.parse(module2SpotbugsXml)

List<NodeChild> module1Nodes = module1Result.depthFirst().toList()
List<NodeChild> module2Nodes = module2Result.depthFirst().toList()
int module1BugCount = module1Nodes.count { NodeChild node -> node.name() == 'BugInstance' }
int module2BugCount = module2Nodes.count { NodeChild node -> node.name() == 'BugInstance' }

println "Module-1 BugInstance count: ${module1BugCount}"
println "Module-2 BugInstance count: ${module2BugCount}"

// The aggregate should contain all bugs from all modules
assert aggregateBugCount == module1BugCount + module2BugCount,
    "Aggregate bug count (${aggregateBugCount}) should equal sum of module bugs (${module1BugCount} + ${module2BugCount})"

// Verify the aggregate FindBugsSummary has the correct total
String aggregateTotalBugs = aggregateResult.FindBugsSummary.@total_bugs.text()
println "Aggregate FindBugsSummary total_bugs: ${aggregateTotalBugs}"
assert aggregateTotalBugs.toInteger() == aggregateBugCount,
    "FindBugsSummary total_bugs (${aggregateTotalBugs}) should match actual BugInstance count (${aggregateBugCount})"

println 'SUCCESS: Aggregate SpotBugs XML verified'
