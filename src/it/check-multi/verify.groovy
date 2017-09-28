/*
 * Copyright (C) 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



//  check module 1


File spotbugXml = new File(basedir, "modules/module-1/target/spotbugsXml.xml")
assert spotbugXml.exists()

def path = new XmlSlurper().parse(spotbugXml)

println '**********************************'
println "Checking Spotbugs Native XML file"
println '**********************************'


allNodes = path.depthFirst().collect { it }
def spotbugsErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${spotbugsErrors}"

assert spotbugsErrors > 0


//  check module 2

spotbugXml = new File(basedir, "modules/module-2/target/spotbugsXml.xml")
assert spotbugXml.exists()

path = new XmlSlurper().parse(spotbugXml)

println '**********************************'
println "Checking Spotbugs Native XML file"
println '**********************************'

allNodes = path.depthFirst().collect { it }
spotbugsErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${spotbugsErrors}"

assert spotbugsErrors > 0
