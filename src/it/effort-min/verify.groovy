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
import java.nio.charset.StandardCharsets

Path spotbugsHtml =  basedir.toPath().resolve('target/site/spotbugs.html')
assert Files.exists(spotbugsHtml)

Path spotbugXdoc = basedir.toPath().resolve('target/spotbugs.xml')
assert Files.exists(spotbugXdoc)

Path spotbugXml = basedir.toPath().resolve('target/spotbugsXml.xml')
assert Files.exists(spotbugXml)

println '******************'
println 'Checking HTML file'
println '******************'

String effortLevel = 'min'

assert spotbugsHtml.getText(StandardCharsets.UTF_8.name()).contains('<i>' + effortLevel + '</i>')

XmlSlurper xmlSlurper = new XmlSlurper()
xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

// Temporarily allow DOCTYPE for HTML parsing
xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', false)
GPathResult path = xmlSlurper.parse(spotbugsHtml)
xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)

int spotbugsErrors = path.body.'**'.find { NodeChild main -> main.@id == 'bodyColumn' }.section[1].table.tr[1].td[1].toInteger()
println "Error Count is ${spotbugsErrors}"

println '*********************************'
println 'Checking Spotbugs Native XML file'
println '*********************************'

path = xmlSlurper.parse(spotbugXml)

List<NodeChild> allNodes = path.depthFirst().toList()
int spotbugsXmlErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
println "BugInstance size is ${spotbugsXmlErrors}"

assert spotbugsErrors == spotbugsXmlErrors

println '******************'
println 'Checking xDoc file'
println '******************'

path = xmlSlurper.parse(spotbugXdoc)

allNodes = path.depthFirst().toList()
int xdocErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
println "BugInstance size is ${xdocErrors}"

assert path.@effort.text() == effortLevel

assert xdocErrors == spotbugsXmlErrors
