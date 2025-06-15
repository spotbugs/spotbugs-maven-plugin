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

//  check module 1

Path spotbugXml = basedir.toPath().resolve("module1/target/spotbugsXml.xml")
assert Files.exists(spotbugXml)

GPathResult path = new XmlSlurper().parse(spotbugXml.toFile())

println '*********************************'
println 'Checking Spotbugs Native XML file'
println '*********************************'


List<Node> allNodes = path.depthFirst().collect { it }
int spotbugsErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${spotbugsErrors}"

assert spotbugsErrors > 0

//  check module 2

spotbugXml = new File(basedir, "module2/target/spotbugsXml.xml")
assert Files.exists(spotbugXml)

path = new XmlSlurper().parse(spotbugXml.toFile())

println '*********************************'
println 'Checking Spotbugs Native XML file'
println '*********************************'

allNodes = path.depthFirst().collect { it }
spotbugsErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${spotbugsErrors}"

assert spotbugsErrors > 0
