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

import org.codehaus.mojo.spotbugs.SourceFileIndexer
import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject
import org.apache.maven.model.Resource
import org.apache.maven.plugin.MojoExecutionException
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class SourceFileIndexerTest extends Specification {

    void "buildListSourceFiles and searchActualFilesLocation work as expected"() {
        given:
        // Setup temp project structure
        File baseDir = Files.createTempDirectory("project").toFile()
        File srcDir = new File(baseDir, "src/main/java")
        srcDir.mkdirs()
        File testFile = new File(srcDir, "Test.java")
        testFile.text = "class Test {}"

        // Mock MavenProject
        MavenProject project = Mock(MavenProject)
        project.getBasedir() >> baseDir
        project.getResources() >> [resource(srcDir)]
        project.getTestResources() >> []
        project.getCompileSourceRoots() >> [srcDir.getAbsolutePath()]
        project.getTestCompileSourceRoots() >> []

        // Mock MavenSession
        MavenSession session = Mock(MavenSession)
        session.getExecutionRootDirectory() >> baseDir.getAbsolutePath()
        session.getCurrentProject() >> project

        SourceFileIndexer indexer = new SourceFileIndexer()

        when:
        indexer.buildListSourceFiles(session)
        String found = indexer.searchActualFilesLocation("Test.java")

        then:
        found != null
        found.endsWith("src/main/java/Test.java")

        cleanup:
        testFile.delete()
        srcDir.delete()
        baseDir.delete()
    }

    void "searchActualFilesLocation throws if not initialized"() {
        given:
        SourceFileIndexer indexer = new SourceFileIndexer()

        when:
        indexer.searchActualFilesLocation("Test.java")

        then:
        thrown(MojoExecutionException)
    }

    void "buildListSourceFiles scans nested subdirectories"() {
        given:
        File baseDir = Files.createTempDirectory("project-nested").toFile()
        File srcDir = new File(baseDir, "src/main/java")
        File subPkg = new File(srcDir, "com/example")
        subPkg.mkdirs()
        File nestedFile = new File(subPkg, "Service.java")
        nestedFile.text = "class Service {}"

        MavenProject project = Mock(MavenProject)
        project.getBasedir() >> baseDir
        project.getResources() >> []
        project.getTestResources() >> []
        project.getCompileSourceRoots() >> [srcDir.getAbsolutePath()]
        project.getTestCompileSourceRoots() >> []

        MavenSession session = Mock(MavenSession)
        session.getExecutionRootDirectory() >> baseDir.getAbsolutePath()
        session.getCurrentProject() >> project

        SourceFileIndexer indexer = new SourceFileIndexer()

        when:
        indexer.buildListSourceFiles(session)
        String found = indexer.searchActualFilesLocation("Service.java")

        then:
        found != null
        found.endsWith("com/example/Service.java")

        cleanup:
        baseDir.deleteDir()
    }

   void "searchActualFilesLocation returns null when file is not found"() {
        given:
        File baseDir = Files.createTempDirectory("project-notfound").toFile()
        File srcDir = new File(baseDir, "src/main/java")
        srcDir.mkdirs()
        File knownFile = new File(srcDir, "Known.java")
        knownFile.text = "class Known {}"

        MavenProject project = Mock(MavenProject)
        project.getBasedir() >> baseDir
        project.getResources() >> []
        project.getTestResources() >> []
        project.getCompileSourceRoots() >> [srcDir.getAbsolutePath()]
        project.getTestCompileSourceRoots() >> []

        MavenSession session = Mock(MavenSession)
        session.getExecutionRootDirectory() >> baseDir.getAbsolutePath()
        session.getCurrentProject() >> project

        SourceFileIndexer indexer = new SourceFileIndexer()
        indexer.buildListSourceFiles(session)

        when:
        String result = indexer.searchActualFilesLocation("DoesNotExist.java")

        then:
        result == null

        cleanup:
        knownFile.delete()
        srcDir.delete()
        baseDir.delete()
    }

    void "buildListSourceFiles indexes test compile source roots"() {
        given:
        File baseDir = Files.createTempDirectory("project-test-roots").toFile()
        File testSrcDir = new File(baseDir, "src/test/java")
        testSrcDir.mkdirs()
        File testFile = new File(testSrcDir, "FooTest.java")
        testFile.text = "class FooTest {}"

        MavenProject project = Mock(MavenProject)
        project.getBasedir() >> baseDir
        project.getResources() >> []
        project.getTestResources() >> [resource(testSrcDir)]
        project.getCompileSourceRoots() >> []
        project.getTestCompileSourceRoots() >> [testSrcDir.getAbsolutePath()]

        MavenSession session = Mock(MavenSession)
        session.getExecutionRootDirectory() >> baseDir.getAbsolutePath()
        session.getCurrentProject() >> project

        SourceFileIndexer indexer = new SourceFileIndexer()

        when:
        indexer.buildListSourceFiles(session)
        String found = indexer.searchActualFilesLocation("FooTest.java")

        then:
        found != null
        found.endsWith("FooTest.java")

        cleanup:
        testFile.delete()
        testSrcDir.delete()
        baseDir.delete()
    }

    void "buildListSourceFiles handles non-existent source directories gracefully"() {
        given:
        File baseDir = Files.createTempDirectory("project-nodir").toFile()

        MavenProject project = Mock(MavenProject)
        project.getBasedir() >> baseDir
        project.getResources() >> [resource(new File(baseDir, "nonexistent/resources"))]
        project.getTestResources() >> []
        project.getCompileSourceRoots() >> [new File(baseDir, "nonexistent/java").getAbsolutePath()]
        project.getTestCompileSourceRoots() >> []

        MavenSession session = Mock(MavenSession)
        session.getExecutionRootDirectory() >> baseDir.getAbsolutePath()
        session.getCurrentProject() >> project

        SourceFileIndexer indexer = new SourceFileIndexer()

        when:
        // No exception should be thrown even when none of the directories exist
        indexer.buildListSourceFiles(session)

        then:
        notThrown(Exception)

        cleanup:
        baseDir.deleteDir()
    }

    void "buildListSourceFiles can be called multiple times resetting the cache each time"() {
        given:
        File baseDir = Files.createTempDirectory("project-reset").toFile()
        File srcDir = new File(baseDir, "src/main/java")
        srcDir.mkdirs()
        File fileA = new File(srcDir, "A.java")
        fileA.text = "class A {}"

        MavenProject project = Mock(MavenProject)
        project.getBasedir() >> baseDir
        project.getResources() >> []
        project.getTestResources() >> []
        project.getCompileSourceRoots() >> [srcDir.getAbsolutePath()]
        project.getTestCompileSourceRoots() >> []

        MavenSession session = Mock(MavenSession)
        session.getExecutionRootDirectory() >> baseDir.getAbsolutePath()
        session.getCurrentProject() >> project

        SourceFileIndexer indexer = new SourceFileIndexer()

        when: "first build"
        indexer.buildListSourceFiles(session)
        String firstResult = indexer.searchActualFilesLocation("A.java")

        then:
        firstResult != null

        when: "second build after adding a new file"
        File fileB = new File(srcDir, "B.java")
        fileB.text = "class B {}"
        indexer.buildListSourceFiles(session)
        String secondResult = indexer.searchActualFilesLocation("B.java")

        then:
        secondResult != null

        cleanup:
        [fileA, fileB, srcDir, baseDir]*.deleteDir()
    }

    private static Resource resource(File dir) {
        Resource res = new Resource()
        res.setDirectory(dir.getAbsolutePath())
        return res
    }

}
