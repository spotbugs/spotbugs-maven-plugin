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

// Verify that spotbugsXml.xml was created - proving SpotBugs ran with noClassOk=true
// even though the project has no Java class files (only resources)

Path spotbugsXml = basedir.toPath().resolve('target/spotbugsXml.xml')
assert Files.exists(spotbugsXml) : 'spotbugsXml.xml should exist when noClassOk=true'

println '**********************************'
println 'Checking Spotbugs Native XML file'
println '**********************************'

XmlSlurper xmlSlurper = new XmlSlurper()
xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

GPathResult path = xmlSlurper.parse(spotbugsXml.toFile())

List<NodeChild> allNodes = path.depthFirst().toList()
int spotbugsXmlErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
println "BugInstance size is ${spotbugsXmlErrors}"

assert spotbugsXmlErrors == 0 : 'Expected 0 bugs when no class files exist'
