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

Path spotbugsHtml =  basedir.toPath().resolve('target/site/spotbugs.html')
assert Files.notExists(spotbugsHtml)

Path spotbugXdoc = basedir.toPath().resolve('target/spotbugs.xml')
assert Files.exists(spotbugXdoc)

Path spotbugXml = basedir.toPath().resolve('target/spotbugsXml.xml')
assert Files.exists(spotbugXml)

println '*********************************'
println 'Checking Spotbugs Native XML file'
println '*********************************'

XmlSlurper xmlSlurper = new XmlSlurper()
xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
GPathResult path =xmlSlurper.parse(spotbugXml)

List<NodeChild> allNodes = path.depthFirst().toList()
int spotbugsXmlErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
println "BugInstance size is ${spotbugsXmlErrors}"

println '******************'
println 'Checking xDoc file'
println '******************'

path = xmlSlurper.parse(spotbugXdoc)

allNodes = path.depthFirst().toList()
int xdocErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
println "BugInstance size is ${xdocErrors}"

assert xdocErrors == spotbugsXmlErrors
