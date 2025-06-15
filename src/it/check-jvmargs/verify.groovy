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

Path spotbugXdoc = basedir.toPath().resolve('target/spotbugs.xml')
assert Files.exists(spotbugXdoc)

Path spotbugXml = basedir.toPath().resolve('target/spotbugsXml.xml')
assert Files.exists(spotbugXml)

println '*********************************'
println 'Checking Spotbugs Native XML file'
println '*********************************'

GPathResult path = new XmlSlurper().parse(spotbugXml.toFile())

List<Node> allNodes = path.depthFirst().collect{ it }
int spotbugsXmlErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${spotbugsXmlErrors}"

println '******************'
println 'Checking xDoc file'
println '******************'

path = new XmlSlurper().parse(spotbugXdoc.toFile())

allNodes = path.depthFirst().collect{ it }
int xdocErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${xdocErrors}"

assert xdocErrors == spotbugsXmlErrors
