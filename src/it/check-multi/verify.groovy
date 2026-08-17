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

//  check module 1

Path spotbugXml = basedir.toPath().resolve('modules/module-1/target/spotbugsXml.xml')
assert Files.exists(spotbugXml)

XmlSlurper xmlSlurper = new XmlSlurper()
xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

GPathResult path = xmlSlurper.parse(spotbugXml)

println '*********************************'
println 'Checking Spotbugs Native XML file'
println '*********************************'

List<NodeChild> allNodes = path.depthFirst().toList()
int spotbugsErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
println "BugInstance size is ${spotbugsErrors}"

assert spotbugsErrors > 0

//  check module 2

spotbugXml = basedir.toPath().resolve('modules/module-2/target/spotbugsXml.xml')
assert Files.exists(spotbugXml)

path = xmlSlurper.parse(spotbugXml)

println '*********************************'
println 'Checking Spotbugs Native XML file'
println '*********************************'

allNodes = path.depthFirst().toList()
spotbugsErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
println "BugInstance size is ${spotbugsErrors}"

assert spotbugsErrors > 0
