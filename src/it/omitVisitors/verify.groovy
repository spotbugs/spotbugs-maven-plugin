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

String effortLevel = 'default'

assert spotbugsHtml.text.contains("<i>" + effortLevel + "</i>")

XmlSlurper xhtmlParser = new XmlSlurper();
xhtmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
xhtmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
GPathResult path = xhtmlParser.parse(spotbugsHtml.toFile())

int spotbugsErrors = path.body.'**'.find {main -> main.@id == 'bodyColumn'}.section[1].table.tr[1].td[1].toInteger()
println "Error Count is ${spotbugsErrors}"

println '*********************************'
println 'Checking Spotbugs Native XML file'
println '*********************************'

path = new XmlSlurper().parse(spotbugXml.toFile())

List<Node> allNodes = path.depthFirst().collect{ it }
int spotbugsXmlErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${spotbugsXmlErrors}"

assert spotbugsXmlErrors == spotbugsErrors

spotbugsXmlErrors = allNodes.findAll {it.name() == 'BugInstance'  && it.@type == "URF_UNREAD_FIELD" }.size()
spotbugsXmlErrors += allNodes.findAll {it.name() == 'BugInstance'  && it.@type == "UUF_UNUSED_FIELD"}.size()
spotbugsXmlErrors += allNodes.findAll {it.name() == 'BugInstance'  && it.@type == "DLS_DEAD_LOCAL_STORE"}.size()
println "BugInstance with includes size is ${spotbugsXmlErrors}"

assert 0 == spotbugsXmlErrors

println '******************'
println 'Checking xDoc file'
println '******************'

path = new XmlSlurper().parse(spotbugXdoc.toFile())

allNodes = path.depthFirst().collect{ it }
int xdocErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${xdocErrors}"

assert xdocErrors == spotbugsErrors

xdocErrors = allNodes.findAll {it.name() == 'BugInstance'  && it.@type == "URF_UNREAD_FIELD" }.size()
xdocErrors += allNodes.findAll {it.name() == 'BugInstance'  && it.@type == "UUF_UNUSED_FIELD"}.size()
xdocErrors += allNodes.findAll {it.name() == 'BugInstance'  && it.@type == "DLS_DEAD_LOCAL_STORE"}.size()
println "BugInstance with includes size is ${xdocErrors}"

assert 0 == xdocErrors
