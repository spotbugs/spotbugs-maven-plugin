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
package org.codehaus.mojo.spotbugs;

import org.apache.maven.doxia.sink.Sink
import org.apache.maven.plugin.logging.Log

import spock.lang.Specification

class SpotbugsReportGeneratorTest extends Specification {

    // Simple stub for ResourceBundle
    static class StubResourceBundle extends ResourceBundle {
        @Override
        protected Object handleGetObject(String key) {
            return key
        }
        @Override
        Enumeration<String> getKeys() {
            return Collections.enumeration([])
        }
        @Override
        Locale getLocale() {
            return Locale.ENGLISH
        }
    }

    void 'generateReport calls all main report sections'() {
        given:
        Sink sink = Mock()
        ResourceBundle bundle = new StubResourceBundle()
        Log log = Mock()
        SpotbugsReportGenerator generator = new SpotbugsReportGenerator(sink, bundle)
        generator.log = log
        generator.threshold = org.codehaus.mojo.spotbugs.SpotBugsInfo.spotbugsThresholds.keySet().first()
        generator.effort = org.codehaus.mojo.spotbugs.SpotBugsInfo.spotbugsEfforts.keySet().first()
        generator.compileSourceRoots = ['src/main/java']
        generator.testSourceRoots = ['src/test/java']
        generator.includeTests = false
        generator.outputDirectory = new File('.')
        generator.xrefLocation = new File('.')
        generator.xrefTestLocation = new File('.')
        generator.spotbugsResults = new groovy.xml.XmlSlurper().parseText('''
            <BugCollection>
                <FindBugsSummary total_classes='1' total_bugs='1'>
                    <PackageStats>
                        <ClassStats class='com.example.Foo' bugs='1'/>
                    </PackageStats>
                </FindBugsSummary>
                <Errors errors='0' missingClasses='0'/>
                <BugInstance type='NP_NULL_ON_SOME_PATH' category='CORRECTNESS' priority='1'>
                    <LongMessage>Null pointer dereference</LongMessage>
                    <Class classname='com.example.Foo'/>
                    <SourceLine classname='com.example.Foo' sourcepath='Foo.java' start='10' end='10'/>
                </BugInstance>
            </BugCollection>
        ''')

        when:
        generator.generateReport()

        then:
        1 * sink.head()
        1 * sink.body()
        3 * sink.section1()
        3 * sink.sectionTitle1()
        3 * sink.table()
        1 * sink.flush()
        1 * sink.close()
    }

}
