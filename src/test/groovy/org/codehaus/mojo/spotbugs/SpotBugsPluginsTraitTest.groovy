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

import spock.lang.Specification
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.logging.Log
import org.apache.maven.execution.MavenSession
import org.codehaus.plexus.resource.ResourceManager

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class SpotBugsPluginsTraitTest extends Specification {

    void "getEffortParameter returns correct effort string"() {
        given:
        Log log = Mock()
        ResourceManager resourceManager = Mock()
        org.eclipse.aether.RepositorySystem repositorySystem = Mock()
        org.apache.maven.repository.RepositorySystem factory = Mock()
        MavenSession session = Mock()
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl(
            effort,
            log,
            resourceManager,
            repositorySystem,
            factory,
            session
        )

        expect:
        impl.getEffortParameter() == expected

        where:
        effort   || expected
        "Max"    || "-effort:max"
        "Min"    || "-effort:min"
        "Normal" || "-effort:default"
        null     || "-effort:default"
    }

    void "isSpotBugsPlugin returns true for JAR containing findbugs.xml at root"() {
        given:
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", Mock(Log), Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))
        Path jarPath = Files.createTempFile("test-plugin", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("findbugs.xml"))
            jos.write("<FindbugsPlugin></FindbugsPlugin>".bytes)
            jos.closeEntry()
        }

        expect:
        impl.isSpotBugsPlugin(jarPath.toFile()) == true

        cleanup:
        Files.deleteIfExists(jarPath)
    }

    void "isSpotBugsPlugin returns false for JAR without META-INF/findbugs/findbugs.xml"() {
        given:
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", Mock(Log), Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))
        Path jarPath = Files.createTempFile("not-a-plugin", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("com/example/Foo.class"))
            jos.write("class".bytes)
            jos.closeEntry()
        }

        expect:
        impl.isSpotBugsPlugin(jarPath.toFile()) == false

        cleanup:
        Files.deleteIfExists(jarPath)
    }

    void "isSpotBugsPlugin returns false for null file"() {
        given:
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", Mock(Log), Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))

        expect:
        impl.isSpotBugsPlugin(null) == false
    }

    void "isSpotBugsPlugin returns false for non-JAR file"() {
        given:
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", Mock(Log), Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))
        Path txtPath = Files.createTempFile("not-a-jar", ".txt")
        txtPath.toFile().text = "hello"

        expect:
        impl.isSpotBugsPlugin(txtPath.toFile()) == false

        cleanup:
        Files.deleteIfExists(txtPath)
    }

    static class SpotBugsPluginsTraitImpl implements SpotBugsPluginsTrait {
        String effort
        String pluginList = ""
        List<PluginArtifact> plugins = []
        List<Artifact> pluginArtifacts = []
        Log log
        File spotbugsXmlOutputDirectory = new File(".")
        ResourceManager resourceManager
        org.eclipse.aether.RepositorySystem repositorySystem
        org.apache.maven.repository.RepositorySystem factory
        MavenSession session

        SpotBugsPluginsTraitImpl(
            String effort,
            Log log,
            ResourceManager resourceManager,
            org.eclipse.aether.RepositorySystem repositorySystem,
            org.apache.maven.repository.RepositorySystem factory,
            MavenSession session
        ) {
            this.effort = effort
            this.log = log
            this.resourceManager = resourceManager
            this.repositorySystem = repositorySystem
            this.factory = factory
            this.session = session
        }
    }

}
