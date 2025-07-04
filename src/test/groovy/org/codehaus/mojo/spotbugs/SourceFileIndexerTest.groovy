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
        found.endsWith("src${File.separator}main${File.separator}java${File.separator}Test.java")

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

    private static Resource resource(File dir) {
        Resource res = new Resource()
        res.setDirectory(dir.getAbsolutePath())
        return res
    }

}
