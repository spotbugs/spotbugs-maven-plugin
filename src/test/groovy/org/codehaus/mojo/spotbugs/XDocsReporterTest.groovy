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
package org.codehaus.mojo.spotbugs

import groovy.xml.XmlSlurper
import groovy.xml.StreamingMarkupBuilder

import java.nio.charset.StandardCharsets

import org.apache.maven.plugin.logging.Log

import spock.lang.Specification

class XDocsReporterTest extends Specification {

    void 'evaluateThresholdParameter returns correct threshold name'() {
        given:
        XDocsReporter reporter = new XDocsReporter(Mock(ResourceBundle), Mock(Log), '1', 'max', StandardCharsets.UTF_8.name())

        expect:
        reporter.evaluateThresholdParameter(input) == expected

        where:
        input | expected
        '1'   | 'High'
        '2'   | 'Normal'
        '3'   | 'Low'
        '4'   | 'Exp'
        '5'   | 'Ignore'
        '99'  | 'Invalid Priority'
    }

    void 'generateReport writes XML output'() {
        given:
        ResourceBundle bundle = Mock(ResourceBundle)
        Log log = Mock(Log)
        StringWriter writer = new StringWriter()
        XDocsReporter reporter = new XDocsReporter(bundle, log, '1', 'max', StandardCharsets.UTF_8.name())
        reporter.outputWriter = writer
        reporter.compileSourceRoots = ['src/main/java']
        reporter.testSourceRoots = ['src/test/java']
        reporter.bugClasses = []
        // Minimal stub for spotbugsResults
        String xml = '''
            <BugCollection>
                <FindBugsSummary total_bugs='0'>
                    <PackageStats>
                        <ClassStats class='com.example.Foo' bugs='0'/>
                    </PackageStats>
                </FindBugsSummary>
                <Error>
                    <analysisError>
                        <message>Error message</message>
                    </analysisError>
                    <MissingClass>com.example.Missing</MissingClass>
                </Error>
            </BugCollection>
        '''
        reporter.spotbugsResults = new XmlSlurper().parseText(xml)

        when:
        reporter.generateReport()
        String output = writer.toString()

        then:
        output.contains('BugCollection')
        output.contains('Project')
        output.contains('Error')
    }

}
