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

import java.nio.file.Files

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.testing.AbstractMojoTestCase

/**
 * Tests for {@link CheckMojo} using the Maven Plugin Testing Harness.
 * Exercises realistic mojo loading and execution paths that Spock mocks cannot reach.
 */
class CheckMojoHarnessTest extends AbstractMojoTestCase {

    private static final String SPOTBUGS_XML_NO_BUGS = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection version="4.9.8" threshold="medium" effort="default">
    <Project/>
    <BugInstance/>
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

    /** Temporary directory used by individual tests. */
    private File tempDir

    @Override
    protected void setUp() throws Exception {
        super.setUp()
        tempDir = Files.createTempDirectory('CheckMojoHarnessTest').toFile()
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown()
        tempDir?.deleteDir()
    }

    /**
     * Verifies that the harness can load the check mojo from the test POM and that
     * key parameters have their expected defaults.
     */
    void testMojoLoadedWithDefaults() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        assert pom.exists(), "Test POM not found: ${pom.absolutePath}"

        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null, 'lookupMojo should return a non-null CheckMojo'

        // Parameters injected from the test POM
        assert mojo.spotbugsXmlOutputFilename == 'spotbugsXml.xml'
        assert mojo.failOnError
        assert mojo.maxAllowedViolations == 0

        // Parameters with default values (not overridden in POM)
        assert !mojo.skip
        assert !mojo.includeTests
        assert !mojo.debug
        assert !mojo.quiet
    }

    /**
     * Verifies that executing the check mojo against a class directory that does not
     * exist causes the mojo to exit early without error (nothing to analyse).
     */
    void testExecuteWithMissingClassDirectory() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null

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
    void testExecuteWithEmptyClassDirectory() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null

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
    void testExecuteWithMissingOutputFile() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null

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
    void testExecuteWithNoBugsInReport() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null

        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = SPOTBUGS_XML_NO_BUGS

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
    void testExecuteWithBugsAndFailOnError() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null

        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = SPOTBUGS_XML_WITH_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = true

        try {
            mojo.execute()
            fail('Expected MojoExecutionException due to bugs in report')
        } catch (MojoExecutionException expected) {
            assert expected.message.contains('failed with')
        }
    }

    /**
     * Verifies that bugs do not trigger a build failure when {@code failOnError=false}.
     */
    void testExecuteWithBugsAndNoFailOnError() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null

        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = SPOTBUGS_XML_WITH_BUGS

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
    void testExecuteWithBugsWithinMaxAllowedViolations() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null

        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = SPOTBUGS_XML_TWO_BUGS

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
    void testExecuteWithInvalidFailThreshold() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null

        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = SPOTBUGS_XML_WITH_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = true
        mojo.failThreshold = 'InvalidPriority'

        try {
            mojo.execute()
            fail('Expected MojoExecutionException for invalid failThreshold')
        } catch (MojoExecutionException expected) {
            assert expected.message.contains('Invalid value for failThreshold')
        }
    }

    /**
     * Verifies that {@code failThreshold=Low} passes bugs that are at or above Low
     * priority (i.e. all bugs) and fails the build when failOnError is set.
     */
    void testExecuteWithFailThresholdLow() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null

        File classesDir = new File(tempDir, 'classes')
        classesDir.mkdirs()
        new File(classesDir, 'Dummy.class').createNewFile()

        File xmlFile = new File(tempDir, 'spotbugsXml.xml')
        xmlFile.text = SPOTBUGS_XML_WITH_BUGS

        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.classFilesDirectory = classesDir
        mojo.testClassFilesDirectory = new File(tempDir, 'test-classes')
        mojo.failOnError = true
        // 'High' priority = index 1, bug has priority "1" (high), so it exceeds the threshold
        mojo.failThreshold = 'Low'

        try {
            mojo.execute()
            fail('Expected MojoExecutionException – High-priority bug exceeds Low threshold')
        } catch (MojoExecutionException expected) {
            assert expected.message.contains('failed with')
        }
    }

    /**
     * Verifies that the mojo skip flag prevents any processing.
     */
    void testExecuteWithSkipEnabled() throws Exception {
        File pom = new File(getBasedir(), 'src/test/resources/unit/check-mojo/minimal-pom.xml')
        CheckMojo mojo = (CheckMojo) lookupMojo('check', pom)
        assert mojo != null

        mojo.skip = true
        mojo.spotbugsXmlOutputDirectory = tempDir
        mojo.classFilesDirectory = new File(tempDir, 'classes')

        // skip=true → must not throw regardless of other state
        mojo.execute()
    }

}
