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

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChild

import java.nio.file.Files
import java.nio.file.Path

Path spotbugsHtml =  basedir.toPath().resolve('target/site/spotbugs.html')
assert Files.exists(spotbugsHtml)

Path spotbugXdoc = basedir.toPath().resolve('target/spotbugs.xml')
assert Files.exists(spotbugXdoc)

Path spotbugXml = basedir.toPath().resolve('target/spotbugsXml.xml')
assert Files.exists(spotbugXml)

println '******************'
println 'Checking HTML file'
println '******************'

XmlSlurper xmlSlurper = new XmlSlurper()
xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

// Temporarily allow DOCTYPE for parsing HTML
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

assert xdocErrors == spotbugsXmlErrors
