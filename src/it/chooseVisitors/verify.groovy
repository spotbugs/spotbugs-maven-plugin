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

Path spotbugsXdoc = basedir.toPath().resolve('target/spotbugs.xml')
assert Files.exists(spotbugsXdoc)

Path spotbugsXml = basedir.toPath().resolve('target/spotbugsXml.xml')
assert Files.exists(spotbugsXml)

println '******************'
println 'Checking HTML file'
println '******************'

String effortLevel = 'default'

assert spotbugsHtml.text.contains('<i>' + effortLevel + '</i>')

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

path = xmlSlurper.parse(spotbugsXml)

List<NodeChild> allNodes = path.depthFirst().toList()
int spotbugsXmlErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
println "BugInstance size is ${spotbugsXmlErrors}"

assert spotbugsXmlErrors == spotbugsErrors

spotbugsXmlErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'URF_UNREAD_FIELD' }
spotbugsXmlErrors += allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'UUF_UNUSED_FIELD'}
spotbugsXmlErrors += allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'DLS_DEAD_LOCAL_STORE'}
println "BugInstance size from detectors removed is ${spotbugsXmlErrors}"

assert 0 == spotbugsXmlErrors

spotbugsXmlErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'NM_METHOD_NAMING_CONVENTION'}
spotbugsXmlErrors += allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'ICAST_INT_CAST_TO_DOUBLE_PASSED_TO_CEIL'}
spotbugsXmlErrors += allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'NM_FIELD_NAMING_CONVENTION'}
println "BugInstance size from detectors removed is ${spotbugsXmlErrors}"

assert 0 != spotbugsXmlErrors

println '******************'
println 'Checking xDoc file'
println '******************'

path = xmlSlurper.parse(spotbugsXdoc)

allNodes = path.depthFirst().toList()
int xdocErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
println "BugInstance size is ${xdocErrors}"

assert xdocErrors == spotbugsErrors

xdocErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'URF_UNREAD_FIELD' }
xdocErrors += allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'UUF_UNUSED_FIELD'}
xdocErrors += allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'DLS_DEAD_LOCAL_STORE'}
println "BugInstance size from detectors removed is ${xdocErrors}"

assert 0 == xdocErrors

xdocErrors = allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'NM_METHOD_NAMING_CONVENTION'}
xdocErrors += allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'ICAST_INT_CAST_TO_DOUBLE_PASSED_TO_CEIL'}
xdocErrors += allNodes.count { NodeChild node -> node.name() == 'BugInstance'  && node.@type == 'NM_FIELD_NAMING_CONVENTION'}
println "BugInstance size from detectors removed is ${xdocErrors}"

assert 0 != spotbugsXmlErrors
