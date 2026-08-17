/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
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
