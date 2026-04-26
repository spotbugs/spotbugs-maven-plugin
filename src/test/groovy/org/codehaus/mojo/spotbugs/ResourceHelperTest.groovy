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
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import org.codehaus.mojo.spotbugs.ResourceHelper
import org.codehaus.mojo.spotbugs.SpotBugsInfo
import org.codehaus.plexus.resource.ResourceManager
import org.apache.maven.plugin.logging.Log

import spock.lang.Specification

class ResourceHelperTest extends Specification {

    void 'getResourceFile returns a file with content from resource and logs debug info'() {
        given:
        Log log = Mock(Log) {
            isDebugEnabled() >> true
        }
        Path outputDirectory = Files.createTempDirectory('ResourceHelperTest')
        ResourceManager resourceManager = Mock(ResourceManager) {
            getResourceAsInputStream(_) >> new ByteArrayInputStream('test'.bytes)
        }
        ResourceHelper helper = new ResourceHelper(log, outputDirectory.toFile(), resourceManager)
        String resource = 'test/path/resource.txt'

        when:
        File result = helper.getResourceFile(resource)

        then:
        result.exists()
        result.toPath() == outputDirectory.resolve('resource.txt')
        Files.readString(result.toPath()) == 'test'
        1 * log.debug({ String msg -> msg.contains("resource is 'test/path/resource.txt'") && msg.contains("location is 'test/path'") && msg.contains("artifact is 'resource.txt'") })

        cleanup:
        result?.delete()
        Files.deleteIfExists(outputDirectory)
    }

    void 'getResourceFile returns an existing file in output directory'() {
        // https://github.com/spotbugs/spotbugs-maven-plugin/issues/1163
        given:
        Log log = Mock(Log) {
            isDebugEnabled() >> true
        }
        Path outputDirectory = Files.createTempDirectory('ResourceHelperTest')
        Path existingFile = outputDirectory.resolve('resource.txt')
        Files.writeString(existingFile, 'originalContent', StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        ResourceManager resourceManager = Mock(ResourceManager) {
            getResourceAsInputStream(_) >> null
        }
        ResourceHelper helper = new ResourceHelper(log, outputDirectory.toFile(), resourceManager)
        String resource = existingFile.toAbsolutePath().toString()

        when:
        File result = helper.getResourceFile(resource)

        then:
        result.exists()
        result.toPath() == existingFile
        Files.readString(result.toPath()) == 'originalContent'

        cleanup:
        result?.delete()
        Files.deleteIfExists(outputDirectory)
    }

    void 'getResourceFile sanitizes special characters in path components'() {
        given:
        Log log = Mock(Log) {
            isDebugEnabled() >> true
        }
        Path outputDirectory = Files.createTempDirectory('ResourceHelperTest-special')
        ResourceManager resourceManager = Mock(ResourceManager) {
            getResourceAsInputStream(_) >> new ByteArrayInputStream('data'.bytes)
        }
        ResourceHelper helper = new ResourceHelper(log, outputDirectory.toFile(), resourceManager)
        // Path with special chars that should be sanitized to underscores
        String resource = 'http://example.com:8080/path?param=value/output.xml'

        when:
        File result = helper.getResourceFile(resource)

        then:
        result.exists()
        result.name == 'output.xml'

        cleanup:
        result?.delete()
        outputDirectory.toFile().deleteDir()
    }

   void 'constructor throws NullPointerException when log is null'() {
        given:
        Path outputDirectory = Files.createTempDirectory('ResourceHelperTest-null-log')
        ResourceManager resourceManager = Mock(ResourceManager)

        when:
        new ResourceHelper(null, outputDirectory.toFile(), resourceManager)

        then:
        thrown(NullPointerException)

        cleanup:
        Files.deleteIfExists(outputDirectory)
    }

    void 'constructor throws NullPointerException when resourceManager is null'() {
        given:
        Log log = Mock(Log)
        Path outputDirectory = Files.createTempDirectory('ResourceHelperTest-null-rm')

        when:
        new ResourceHelper(log, outputDirectory.toFile(), null)

        then:
        thrown(NullPointerException)

        cleanup:
        Files.deleteIfExists(outputDirectory)
    }

    void 'getResourceFile throws NullPointerException when resource is null'() {
        given:
        Log log = Mock(Log)
        Path outputDirectory = Files.createTempDirectory('ResourceHelperTest-null-res')
        ResourceManager resourceManager = Mock(ResourceManager)
        ResourceHelper helper = new ResourceHelper(log, outputDirectory.toFile(), resourceManager)

        when:
        helper.getResourceFile(null)

        then:
        thrown(NullPointerException)

        cleanup:
        Files.deleteIfExists(outputDirectory)
    }

    void 'getResourceFile handles resource without path separator'() {
        given:
        Log log = Mock(Log) {
            isDebugEnabled() >> false
        }
        Path outputDirectory = Files.createTempDirectory('ResourceHelperTest-no-sep')
        ResourceManager resourceManager = Mock(ResourceManager) {
            getResourceAsInputStream(_) >> new ByteArrayInputStream('content'.bytes)
        }
        ResourceHelper helper = new ResourceHelper(log, outputDirectory.toFile(), resourceManager)

        when:
        File result = helper.getResourceFile('resource.txt')

        then:
        result.exists()
        Files.readString(result.toPath()) == 'content'

        cleanup:
        result?.delete()
        Files.deleteIfExists(outputDirectory)
    }

    void 'getResourceFile throws MojoExecutionException when reading the resource stream throws IOException'() {
        given:
        Log log = Mock(Log) {
            isDebugEnabled() >> false
            isErrorEnabled() >> true
        }
        Path outputDirectory = Files.createTempDirectory('ResourceHelperTest-ioex')
        // Return an InputStream whose read() immediately throws IOException
        InputStream failingStream = new InputStream() {
            @Override
            int read() throws IOException {
                throw new IOException('simulated read error')
            }
        }
        ResourceManager resourceManager = Mock(ResourceManager) {
            getResourceAsInputStream(_) >> failingStream
        }
        ResourceHelper helper = new ResourceHelper(log, outputDirectory.toFile(), resourceManager)

        when:
        helper.getResourceFile('some/path/resource.xml')

        then:
        thrown(org.apache.maven.plugin.MojoExecutionException)

        cleanup:
        outputDirectory.toFile().deleteDir()
    }

    void 'getResourceFile works without debug logging enabled'() {
        given:
        Log log = Mock(Log) {
            isDebugEnabled() >> false
        }
        Path outputDirectory = Files.createTempDirectory('ResourceHelperTest-no-debug')
        ResourceManager resourceManager = Mock(ResourceManager) {
            getResourceAsInputStream(_) >> new ByteArrayInputStream('nodebug'.bytes)
        }
        ResourceHelper helper = new ResourceHelper(log, outputDirectory.toFile(), resourceManager)

        when:
        File result = helper.getResourceFile('some/path/resource.txt')

        then:
        result.exists()
        Files.readString(result.toPath()) == 'nodebug'
        0 * log.debug(_)

        cleanup:
        result?.delete()
        Files.deleteIfExists(outputDirectory)
    }

}
