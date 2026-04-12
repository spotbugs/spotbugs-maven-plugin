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

import org.apache.maven.api.plugin.testing.InjectMojo
import org.apache.maven.api.plugin.testing.MojoParameter
import org.apache.maven.api.plugin.testing.MojoTest
import org.apache.maven.plugin.MojoExecutionException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for {@link VerifyMojo} using the Maven Plugin Testing Harness (JUnit 5).
 * <p>
 * The {@code verify} goal reads a pre-existing SpotBugs XML file and fails the build
 * if violations are found. It does not invoke SpotBugs itself.
 */
@MojoTest
class VerifyMojoHarnessTest {

    private static final String POM = 'src/test/resources/unit/check-mojo/minimal-pom.xml'

    private static final String SPOTBUGS_XML_NO_BUGS = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection version="4.9.8" threshold="medium" effort="default">
    <Project/>
    <Errors errors="0" missingClasses="0"/>
    <FindBugsSummary total_classes="1" total_bugs="0" total_size="100"/>
    <ClassFeatures/>
    <History/>
</BugCollection>
'''

    private static final String SPOTBUGS_XML_WITH_BUGS = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection version="4.9.8" threshold="medium" effort="default">
    <Project/>
    <BugInstance type="NP_NULL_ON_SOME_PATH" priority="1" rank="1" abbrev="NP" category="CORRECTNESS">
        <LongMessage>Null pointer dereference</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="10" end="10">
            <Message>At Foo.java:[line 10]</Message>
        </SourceLine>
    </BugInstance>
    <Errors errors="0" missingClasses="0"/>
    <FindBugsSummary total_classes="1" total_bugs="1"/>
</BugCollection>
'''

    private static final String SPOTBUGS_XML_TWO_BUGS = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection version="4.9.8" threshold="medium" effort="default">
    <Project/>
    <BugInstance type="NP_NULL_ON_SOME_PATH" priority="1" rank="1" abbrev="NP" category="CORRECTNESS">
        <LongMessage>Null pointer dereference at line 10</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="10" end="10">
            <Message>At Foo.java:[line 10]</Message>
        </SourceLine>
    </BugInstance>
    <BugInstance type="NP_NULL_ON_SOME_PATH" priority="2" rank="5" abbrev="NP" category="CORRECTNESS">
        <LongMessage>Null pointer dereference at line 20</LongMessage>
        <SourceLine classname="com.example.Foo" sourcepath="Foo.java" start="20" end="20">
            <Message>At Foo.java:[line 20]</Message>
        </SourceLine>
    </BugInstance>
    <Errors errors="0" missingClasses="0"/>
    <FindBugsSummary total_classes="1" total_bugs="2"/>
</BugCollection>
'''

    /**
     * Verifies that the harness can load the verify mojo from the test POM and that
     * the mojo is an instance of {@link VerifyMojo}.
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    void mojoLoadedSuccessfully(VerifyMojo mojo) {
        assertNotNull(mojo, 'InjectMojo should supply a non-null VerifyMojo')
        assertInstanceOf(VerifyMojo, mojo)
        assertInstanceOf(BaseViolationCheckMojo, mojo)
    }

    /**
     * Verifies that loaded mojo has expected parameter values from the test POM.
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    void mojoLoadedWithDefaults(VerifyMojo mojo) {
        assertEquals('spotbugsXml.xml', mojo.spotbugsXmlOutputFilename)
        assertTrue(mojo.failOnError)
        assertEquals(0, mojo.maxAllowedViolations)
        assertFalse(mojo.skip)
        assertFalse(mojo.includeTests)
        assertFalse(mojo.debug)
        assertFalse(mojo.quiet)
    }

    /**
     * Verifies that the verify mojo exits early when there are no class files to
     * inspect (the class directory does not exist).
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    void executeWithMissingClassDirectory(VerifyMojo mojo, @TempDir File tempDir) {
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.classFilesDirectory = new File(tempDir, 'nonexistent-classes')
        mojo.testClassFilesDirectory = new File(tempDir, 'nonexistent-test-classes')

        // Should complete without throwing – nothing to do
        mojo.execute()
    }

    /**
     * Verifies that executing the verify mojo with an empty class directory exits
     * early without error.
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    void executeWithEmptyClassDirectory(VerifyMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')

        mojo.execute()
    }

    /**
     * Verifies that when class files exist but the SpotBugs XML output file is absent
     * the mojo logs a warning and returns gracefully.
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    void executeWithMissingOutputFile(VerifyMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')

        // No XML file present – mojo should warn and return without throwing
        mojo.execute()
    }

    /**
     * Verifies that when the SpotBugs XML reports zero bugs the mojo completes
     * without throwing.
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    void executeWithNoBugsInReport(VerifyMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        new File(tempDir, 'spotbugsXml.xml').text = SPOTBUGS_XML_NO_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = false

        mojo.execute()
    }

    /**
     * Verifies that a report containing bugs causes the mojo to throw
     * {@link MojoExecutionException} when {@code failOnError=true}.
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    void executeWithBugsAndFailOnError(VerifyMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        new File(tempDir, 'spotbugsXml.xml').text = SPOTBUGS_XML_WITH_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = true

        MojoExecutionException ex = assertThrows(MojoExecutionException) { mojo.execute() }
        assertTrue(ex.message.contains('failed with'), "Expected 'failed with' in: ${ex.message}")
    }

    /**
     * Verifies that bugs do not trigger a build failure when {@code failOnError=false}.
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    void executeWithBugsAndNoFailOnError(VerifyMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        new File(tempDir, 'spotbugsXml.xml').text = SPOTBUGS_XML_WITH_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = false

        // failOnError=false → should not throw even with bugs present
        mojo.execute()
    }

    /**
     * Verifies that when the number of bugs is within {@code maxAllowedViolations}
     * the mojo does not throw.
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    void executeWithBugsWithinMaxAllowedViolations(VerifyMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        new File(tempDir, 'spotbugsXml.xml').text = SPOTBUGS_XML_TWO_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = true
        mojo.maxAllowedViolations = 5   // 2 bugs ≤ 5 allowed → no exception

        mojo.execute()
    }

    /**
     * Verifies that the mojo skip flag prevents any processing.
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    @MojoParameter(name = 'skip', value = 'true')
    void executeWithSkipEnabled(VerifyMojo mojo, @TempDir File tempDir) {
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.classFilesDirectory = new File(tempDir, 'classes')

        // skip=true → must not throw regardless of other state
        mojo.execute()
    }

    /**
     * Verifies that an invalid {@code failThreshold} value causes the mojo to throw.
     */
    @Test
    @InjectMojo(goal = 'verify', pom = POM)
    void executeWithInvalidFailThreshold(VerifyMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        new File(tempDir, 'spotbugsXml.xml').text = SPOTBUGS_XML_WITH_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = true
        mojo.failThreshold = 'InvalidPriority'

        MojoExecutionException ex = assertThrows(MojoExecutionException) { mojo.execute() }
        assertTrue(ex.message.contains('Invalid value for failThreshold'),
            "Expected 'Invalid value for failThreshold' in: ${ex.message}")
    }

}
