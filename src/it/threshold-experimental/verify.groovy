/*
 * Copyright (C) 2006-2017 the original author or authors.
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

def thresholdLevel = 'experimental'


File spotbugsHtml =  new File(basedir, 'target/site/spotbugs.html')
assert spotbugsHtml.exists()

File spotbugXdoc = new File(basedir, 'target/spotbugs.xml')
assert spotbugXdoc.exists()

File spotbugXml = new File(basedir, 'target/spotbugsXml.xml')
assert spotbugXml.exists()


println '***************************'
println "Checking HTML file"
println '***************************'

assert spotbugsHtml.text.contains( "<i>" + thresholdLevel + "</i>" )

def xhtmlParser = new XmlSlurper();xhtmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);xhtmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);def path = xhtmlParser.parse( spotbugsHtml )
//*[@id="contentBox"]/div[2]/table/tbody/tr[2]/td[2]
def spotbugsErrors = path.body.'**'.find {div -> div.@id == 'contentBox'}.div[1].table.tr[1].td[1].toInteger()
println "Error Count is ${spotbugsErrors}"


println '**********************************'
println "Checking Spotbugs Native XML file"
println '**********************************'

path = new XmlSlurper().parse(spotbugXml)

allNodes = path.depthFirst().collect{ it }
def spotbugsXmlErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${spotbugsXmlErrors}"


println '***************************'
println "Checking xDoc file"
println '***************************'

path = new XmlSlurper().parse(spotbugXdoc)

allNodes = path.depthFirst().collect{ it }
def xdocErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${xdocErrors}"


assert xdocErrors == spotbugsXmlErrors

assert spotbugsErrors == spotbugsXmlErrors

