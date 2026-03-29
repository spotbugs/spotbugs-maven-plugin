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
 * Tests for {@link CheckMojo} using the Maven Plugin Testing Harness (JUnit 5).
 * Exercises realistic mojo loading and execution paths that Spock mocks cannot reach.
 */
@MojoTest
class CheckMojoHarnessTest {

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
     * Verifies that the harness can load the check mojo from the test POM and that
     * key parameters have their expected defaults.
     */
    @Test
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    void mojoLoadedWithDefaults(CheckMojo mojo) {
        assertNotNull(mojo, 'InjectMojo should supply a non-null CheckMojo')

        // Parameters injected from the test POM
        assertEquals('spotbugsXml.xml', mojo.spotbugsXmlOutputFilename)
        assertTrue(mojo.failOnError)
        assertEquals(0, mojo.maxAllowedViolations)

        // Parameters with default values (not overridden in POM)
        assertFalse(mojo.skip)
        assertFalse(mojo.includeTests)
        assertFalse(mojo.debug)
        assertFalse(mojo.quiet)
    }

    /**
     * Verifies that executing the check mojo against a class directory that does not
     * exist causes the mojo to exit early without error (nothing to analyse).
     */
    @Test
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    void executeWithMissingClassDirectory(CheckMojo mojo, @TempDir File tempDir) {
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.classFilesDirectory = new File(tempDir, 'nonexistent-classes')
        mojo.testClassFilesDirectory = new File(tempDir, 'nonexistent-test-classes')

        // Should complete without throwing – no class files means nothing to do
        mojo.execute()
    }

    /**
     * Verifies that executing the check mojo against an empty class directory
     * causes the mojo to exit early without error.
     */
    @Test
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    void executeWithEmptyClassDirectory(CheckMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')

        // Empty class directory → nothing to analyse → no exception
        mojo.execute()
    }

    /**
     * Verifies that when class files exist but the SpotBugs XML output file is absent
     * the mojo logs a warning and returns gracefully.
     */
    @Test
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    void executeWithMissingOutputFile(CheckMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')

        // The XML file does not exist: mojo should warn and return without throwing
        mojo.execute()
    }

    /**
     * Verifies that when the SpotBugs XML reports zero bugs the mojo completes
     * without throwing and logs that no bugs were found.
     */
    @Test
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    void executeWithNoBugsInReport(CheckMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        new File(tempDir, 'spotbugsXml.xml').text = SPOTBUGS_XML_NO_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = false

        // No bugs → should not throw
        mojo.execute()
    }

    /**
     * Verifies that a report containing bugs causes the mojo to throw
     * {@link MojoExecutionException} when {@code failOnError=true}.
     */
    @Test
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    void executeWithBugsAndFailOnError(CheckMojo mojo, @TempDir File tempDir) {
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
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    void executeWithBugsAndNoFailOnError(CheckMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        new File(tempDir, 'spotbugsXml.xml').text = SPOTBUGS_XML_WITH_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = false

        // failOnError=false → should not throw even with bugs
        mojo.execute()
    }

    /**
     * Verifies that when the number of bugs is within {@code maxAllowedViolations}
     * the mojo logs them but does not throw.
     */
    @Test
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    void executeWithBugsWithinMaxAllowedViolations(CheckMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        new File(tempDir, 'spotbugsXml.xml').text = SPOTBUGS_XML_TWO_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = true
        // Allow up to 5 violations – the report has 2, so no exception expected
        mojo.maxAllowedViolations = 5

        mojo.execute()
    }

    /**
     * Verifies that an invalid {@code failThreshold} value causes the mojo to throw.
     */
    @Test
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    void executeWithInvalidFailThreshold(CheckMojo mojo, @TempDir File tempDir) {
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

    /**
     * Verifies that {@code failThreshold=Low} passes bugs that are at or above Low
     * priority (i.e. all bugs) and fails the build when failOnError is set.
     */
    @Test
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    void executeWithFailThresholdLow(CheckMojo mojo, @TempDir File tempDir) {
        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        new File(tempDir, 'spotbugsXml.xml').text = SPOTBUGS_XML_WITH_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = true
        // 'High' priority = index 1, bug has priority "1" (high), so it exceeds the threshold
        mojo.failThreshold = 'Low'

        MojoExecutionException ex = assertThrows(MojoExecutionException) { mojo.execute() }
        assertTrue(ex.message.contains('failed with'), "Expected 'failed with' in: ${ex.message}")
    }

    /**
     * Verifies that the mojo skip flag prevents any processing.
     */
    @Test
    @InjectMojo(goal = 'check', pom = 'src/test/resources/unit/check-mojo/minimal-pom.xml')
    @MojoParameter(name = 'skip', value = 'true')
    void executeWithSkipEnabled(CheckMojo mojo, @TempDir File tempDir) {
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.classFilesDirectory = new File(tempDir, 'classes')

        // skip=true → must not throw regardless of other state
        mojo.execute()
    }

}
