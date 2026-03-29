/*
 * Copyright 2005-2026 the original author or authors.
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

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.logging.Log

import spock.lang.Specification
import spock.lang.TempDir

class BaseViolationCheckMojoTest extends Specification {

    @TempDir
    File tempDir

    // Minimal concrete subclass – delegates execute() to the base implementation.
    static class ConcreteCheckMojo extends BaseViolationCheckMojo {
        @Override
        void execute() {
            super.execute()
        }
    }

    // Legacy no-op subclass kept for backward-compatible tests.
    static class TestMojo extends BaseViolationCheckMojo {
        @Override
        void execute() { /* no-op for test */ }
    }

    // -------------------------------------------------------------------------
    // Basic structural / property tests (pre-existing)
    // -------------------------------------------------------------------------

    void 'should extend AbstractMojo'() {
        expect:
        AbstractMojo.isAssignableFrom(BaseViolationCheckMojo)
    }

    void 'should have default property values not injected'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.spotbugsXmlOutputFilename == null
        mojo.includeTests == false
        mojo.debug == false
        mojo.skip == false
        mojo.failOnError == false
        mojo.maxAllowedViolations == 0
        mojo.quiet == false
    }

    void 'should allow setting properties'() {
        given:
        TestMojo mojo = new TestMojo()

        when:
        mojo.spotbugsXmlOutputFilename = 'output.xml'
        mojo.includeTests = true
        mojo.debug = true
        mojo.skip = true
        mojo.failOnError = true
        mojo.maxAllowedViolations = 5
        mojo.quiet = true

        then:
        mojo.spotbugsXmlOutputFilename == 'output.xml'
        mojo.includeTests
        mojo.debug
        mojo.skip
        mojo.failOnError
        mojo.maxAllowedViolations == 5
        mojo.quiet
    }

    void 'should not throw when execute is called'() {
        given:
        TestMojo mojo = new TestMojo()

        when:
        mojo.execute()

        then:
        notThrown(Exception)
    }

    // -------------------------------------------------------------------------
    // execute() – skip path
    // -------------------------------------------------------------------------

    void 'execute() returns early and logs info when skip=true'() {
        given:
        Log log = Mock(Log)
        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.skip = true
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.classFilesDirectory = new File(tempDir, 'classes')
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')

        when:
        mojo.execute()

        then:
        1 * log.info('Spotbugs plugin skipped')
    }

    // -------------------------------------------------------------------------
    // execute() – no class files paths
    // -------------------------------------------------------------------------

    void 'execute() exits early when class files directory does not exist'() {
        given:
        Log log = Mock(Log)
        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.classFilesDirectory = new File(tempDir, 'nonexistent')
        mojo.testClassFilesDirectory = new File(tempDir, 'nonexistent-tests')

        when:
        mojo.execute()

        then:
        0 * log.info('Spotbugs plugin skipped')
        0 * log.warn(_)
    }

    void 'execute() exits early when class files directory is empty'() {
        given:
        Log log = Mock(Log)
        File classesDir = new File(tempDir, 'empty-classes')
        classesDir.mkdirs()

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'nonexistent-tests')

        when:
        mojo.execute()

        then:
        0 * log.warn(_)
    }

    void 'execute() scans test classes when includeTests=true'() {
        given:
        Log log = Mock(Log)
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        File testClassesDir = new File(tempDir, 'test-classes')
        testClassesDir.mkdirs()
        new File(testClassesDir, 'TestFoo.class').createNewFile()

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.includeTests = true
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = testClassesDir

        when:
        // No XML file, so it will warn and return – the important thing is it reached the XML check
        mojo.execute()

        then:
        1 * log.warn('Output file does not exist!')
    }

    // -------------------------------------------------------------------------
    // execute() – XML output file missing / present but no bugs
    // -------------------------------------------------------------------------

    void 'execute() warns and returns when XML output file is missing'() {
        given:
        Log log = Mock(Log)
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Foo.class').createNewFile()

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'no-test-classes')

        when:
        mojo.execute()

        then:
        1 * log.warn('Output file does not exist!')
    }

    void 'execute() logs no-bugs message when XML contains zero bugs'() {
        given:
        Log log = Mock(Log) {
            isInfoEnabled() >> true
        }
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Foo.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection>
    <Errors errors="0" missingClasses="0"/>
</BugCollection>
'''

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'no-test-classes')

        when:
        mojo.execute()

        then:
        1 * log.info('No errors/warnings found')
    }

    // -------------------------------------------------------------------------
    // execute() – bugs present, various configurations
    // -------------------------------------------------------------------------

    void 'execute() throws MojoExecutionException when bugs exist and failOnError=true'() {
        given:
        Log log = Mock(Log) {
            isInfoEnabled() >> true
            isErrorEnabled() >> true
        }
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Foo.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection>
    <BugInstance type="NP_NULL" priority="1">
        <LongMessage>Null pointer dereference</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="5" end="5">
            <Message>At Foo.java:[line 5]</Message>
        </SourceLine>
    </BugInstance>
    <Errors errors="0" missingClasses="0"/>
</BugCollection>
'''

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'no-test-classes')
        mojo.failOnError = true

        when:
        mojo.execute()

        then:
        MojoExecutionException ex = thrown(MojoExecutionException)
        ex.message.contains('failed with')
    }

    void 'execute() does not throw when bugs exist and failOnError=false'() {
        given:
        Log log = Mock(Log) {
            isInfoEnabled() >> true
            isErrorEnabled() >> true
        }
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Foo.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection>
    <BugInstance type="NP_NULL" priority="1">
        <LongMessage>Null pointer dereference</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="5" end="5">
            <Message>At Foo.java:[line 5]</Message>
        </SourceLine>
    </BugInstance>
    <Errors errors="0" missingClasses="0"/>
</BugCollection>
'''

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'no-test-classes')
        mojo.failOnError = false

        when:
        mojo.execute()

        then:
        notThrown(MojoExecutionException)
    }

    void 'execute() suppresses build failure when bug count is within maxAllowedViolations'() {
        given:
        Log log = Mock(Log) {
            isInfoEnabled() >> true
            isErrorEnabled() >> true
        }
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Foo.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection>
    <BugInstance type="NP_NULL" priority="1">
        <LongMessage>Bug one</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="1" end="1">
            <Message>At Foo.java:[line 1]</Message>
        </SourceLine>
    </BugInstance>
    <BugInstance type="NP_NULL" priority="2">
        <LongMessage>Bug two</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="2" end="2">
            <Message>At Foo.java:[line 2]</Message>
        </SourceLine>
    </BugInstance>
    <Errors errors="0" missingClasses="0"/>
</BugCollection>
'''

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'no-test-classes')
        mojo.failOnError = true
        mojo.maxAllowedViolations = 5   // 2 bugs ≤ 5 allowed → should NOT throw

        when:
        mojo.execute()

        then:
        notThrown(MojoExecutionException)
    }

    void 'execute() throws when bug count exceeds maxAllowedViolations'() {
        given:
        Log log = Mock(Log) {
            isInfoEnabled() >> true
            isErrorEnabled() >> true
        }
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Foo.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection>
    <BugInstance type="NP_NULL" priority="1">
        <LongMessage>Bug one</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="1" end="1">
            <Message>At Foo.java:[line 1]</Message>
        </SourceLine>
    </BugInstance>
    <BugInstance type="NP_NULL" priority="2">
        <LongMessage>Bug two</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="2" end="2">
            <Message>At Foo.java:[line 2]</Message>
        </SourceLine>
    </BugInstance>
    <Errors errors="0" missingClasses="0"/>
</BugCollection>
'''

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'no-test-classes')
        mojo.failOnError = true
        mojo.maxAllowedViolations = 1   // 2 bugs > 1 allowed → should throw

        when:
        mojo.execute()

        then:
        thrown(MojoExecutionException)
    }

    // -------------------------------------------------------------------------
    // execute() – failThreshold
    // -------------------------------------------------------------------------

    void 'execute() throws for invalid failThreshold value'() {
        given:
        Log log = Mock(Log) {
            isInfoEnabled() >> true
        }
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Foo.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection>
    <BugInstance type="NP_NULL" priority="1">
        <LongMessage>Null pointer</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="1" end="1">
            <Message>At Foo.java:[line 1]</Message>
        </SourceLine>
    </BugInstance>
    <Errors errors="0" missingClasses="0"/>
</BugCollection>
'''

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'no-test-classes')
        mojo.failOnError = true
        mojo.failThreshold = 'NotAPriority'

        when:
        mojo.execute()

        then:
        MojoExecutionException ex = thrown(MojoExecutionException)
        ex.message.contains('Invalid value for failThreshold')
    }

    void 'execute() does not throw when all bugs are below failThreshold'() {
        given:
        Log log = Mock(Log) {
            isInfoEnabled() >> true
        }
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Foo.class').createNewFile()

        // Only a Low-priority bug (priority=3), threshold = Medium (priority=2)
        // Low (3) > Medium (2), so no bugs are "at or above" the threshold → no failure
        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection>
    <BugInstance type="NP_NULL" priority="3">
        <LongMessage>Low priority bug</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="1" end="1">
            <Message>At Foo.java:[line 1]</Message>
        </SourceLine>
    </BugInstance>
    <Errors errors="0" missingClasses="0"/>
</BugCollection>
'''

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'no-test-classes')
        mojo.failOnError = true
        mojo.failThreshold = 'Medium'   // only bugs with priority ≤ 2 trigger failure

        when:
        mojo.execute()

        then:
        notThrown(MojoExecutionException)
    }

    // -------------------------------------------------------------------------
    // execute() – quiet flag
    // -------------------------------------------------------------------------

    void 'execute() does not log individual bugs when quiet=true'() {
        given:
        Log log = Mock(Log) {
            isInfoEnabled() >> true
        }
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Foo.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection>
    <BugInstance type="NP_NULL" priority="1">
        <LongMessage>Quiet bug</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="1" end="1">
            <Message>At Foo.java:[line 1]</Message>
        </SourceLine>
    </BugInstance>
    <Errors errors="0" missingClasses="0"/>
</BugCollection>
'''

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'no-test-classes')
        mojo.failOnError = false
        mojo.quiet = true

        when:
        mojo.execute()

        then:
        // quiet=true means no individual bug messages logged
        0 * log.error(_)
    }

    // -------------------------------------------------------------------------
    // execute() – debug logging path
    // -------------------------------------------------------------------------

    void 'execute() logs file-walking details when debug is enabled'() {
        given:
        Log log = Mock(Log) {
            isDebugEnabled() >> true
            isInfoEnabled() >> true
        }
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Foo.class').createNewFile()

        ConcreteCheckMojo mojo = new ConcreteCheckMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'no-test-classes')

        when:
        // No XML file → warns and returns, but we want to verify debug path was exercised
        mojo.execute()

        then:
        (1.._) * log.debug(_)
    }

}
