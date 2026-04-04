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

    void "isSpotBugsPlugin returns false for JAR without findbugs.xml"() {
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

    void "buildBugTypeUrlMap returns empty map when no plugins are configured"() {
        given:
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", Mock(Log), Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))

        expect:
        impl.buildBugTypeUrlMap(null).isEmpty()
    }

    void "buildBugTypeUrlMap maps bug types from a known plugin using built-in defaults"() {
        given:
        Log log = Mock()
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))

        // Create a fake fb-contrib plugin JAR with a findbugs.xml that declares two bug patterns
        Path jarPath = Files.createTempFile("fb-contrib", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("findbugs.xml"))
            jos.write('''<FindbugsPlugin pluginid="com.mebigfatguy.fbcontrib">
                <BugPattern type="ABC_ARRAY_BASED_COLLECTIONS" abbrev="ABC" category="PERFORMANCE"/>
                <BugPattern type="SPP_STRINGBUILDER_IS_MUTABLE" abbrev="SPP" category="STYLE"/>
            </FindbugsPlugin>'''.bytes)
            jos.closeEntry()
        }
        impl.pluginList = jarPath.toAbsolutePath().toString()

        when:
        Map<String, String> result = impl.buildBugTypeUrlMap(null)

        then:
        result['ABC_ARRAY_BASED_COLLECTIONS'] == 'https://fb-contrib.sourceforge.net/bugdescriptions.html#ABC_ARRAY_BASED_COLLECTIONS'
        result['SPP_STRINGBUILDER_IS_MUTABLE'] == 'https://fb-contrib.sourceforge.net/bugdescriptions.html#SPP_STRINGBUILDER_IS_MUTABLE'

        cleanup:
        Files.deleteIfExists(jarPath)
    }

    void "buildBugTypeUrlMap respects user-supplied URL override for a known plugin"() {
        given:
        Log log = Mock()
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))

        Path jarPath = Files.createTempFile("fb-contrib-override", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("findbugs.xml"))
            jos.write('''<FindbugsPlugin pluginid="com.mebigfatguy.fbcontrib">
                <BugPattern type="ABC_ARRAY_BASED_COLLECTIONS" abbrev="ABC" category="PERFORMANCE"/>
            </FindbugsPlugin>'''.bytes)
            jos.closeEntry()
        }
        impl.pluginList = jarPath.toAbsolutePath().toString()

        when:
        Map<String, String> result = impl.buildBugTypeUrlMap(
            ['com.mebigfatguy.fbcontrib': 'https://custom.example.com/bugs#{type}'])

        then:
        result['ABC_ARRAY_BASED_COLLECTIONS'] == 'https://custom.example.com/bugs#ABC_ARRAY_BASED_COLLECTIONS'

        cleanup:
        Files.deleteIfExists(jarPath)
    }

    void "buildBugTypeUrlMap supports user-supplied URL for an unknown plugin"() {
        given:
        Log log = Mock()
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))

        Path jarPath = Files.createTempFile("custom-plugin", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("findbugs.xml"))
            jos.write('''<FindbugsPlugin pluginid="com.example.myplugin">
                <BugPattern type="MY_BUG_TYPE" abbrev="MBT" category="STYLE"/>
            </FindbugsPlugin>'''.bytes)
            jos.closeEntry()
        }
        impl.pluginList = jarPath.toAbsolutePath().toString()

        when:
        Map<String, String> result = impl.buildBugTypeUrlMap(
            ['com.example.myplugin': 'https://example.com/bugs#{type}'])

        then:
        result['MY_BUG_TYPE'] == 'https://example.com/bugs#MY_BUG_TYPE'

        cleanup:
        Files.deleteIfExists(jarPath)
    }

    void "buildBugTypeUrlMap ignores plugin JARs without a configured URL"() {
        given:
        Log log = Mock()
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))

        Path jarPath = Files.createTempFile("unknown-plugin", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("findbugs.xml"))
            jos.write('''<FindbugsPlugin pluginid="com.example.unknownplugin">
                <BugPattern type="UNKNOWN_BUG" abbrev="UB" category="STYLE"/>
            </FindbugsPlugin>'''.bytes)
            jos.closeEntry()
        }
        impl.pluginList = jarPath.toAbsolutePath().toString()

        when:
        Map<String, String> result = impl.buildBugTypeUrlMap(null)

        then:
        !result.containsKey('UNKNOWN_BUG')

        cleanup:
        Files.deleteIfExists(jarPath)
    }

    void "buildBugTypeUrlMap with pluginArtifacts maps bug types from non-spotbugs artifact"() {
        given:
        Log log = Mock()
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))

        Path jarPath = Files.createTempFile("artifact-plugin", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("findbugs.xml"))
            jos.write('''<FindbugsPlugin pluginid="com.mebigfatguy.fbcontrib">
                <BugPattern type="ABC_ARRAY_BASED_COLLECTIONS" abbrev="ABC" category="PERFORMANCE"/>
            </FindbugsPlugin>'''.bytes)
            jos.closeEntry()
        }

        Artifact artifact = Mock(Artifact)
        artifact.groupId >> "com.mebigfatguy"
        artifact.file >> jarPath.toFile()
        impl.pluginArtifacts = [artifact]

        when:
        Map<String, String> result = impl.buildBugTypeUrlMap(null)

        then:
        result['ABC_ARRAY_BASED_COLLECTIONS'] == 'https://fb-contrib.sourceforge.net/bugdescriptions.html#ABC_ARRAY_BASED_COLLECTIONS'

        cleanup:
        Files.deleteIfExists(jarPath)
    }

    void "buildBugTypeUrlMap skips artifacts from com.github.spotbugs group"() {
        given:
        Log log = Mock()
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))

        Path jarPath = Files.createTempFile("spotbugs-core", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("findbugs.xml"))
            jos.write('''<FindbugsPlugin pluginid="com.github.spotbugs">
                <BugPattern type="CORE_BUG" abbrev="CB" category="CORRECTNESS"/>
            </FindbugsPlugin>'''.bytes)
            jos.closeEntry()
        }

        Artifact artifact = Mock(Artifact)
        artifact.groupId >> "com.github.spotbugs"
        artifact.file >> jarPath.toFile()
        impl.pluginArtifacts = [artifact]

        when:
        Map<String, String> result = impl.buildBugTypeUrlMap(null)

        then:
        result.isEmpty()

        cleanup:
        Files.deleteIfExists(jarPath)
    }

    // -------------------------------------------------------------------------
    // getSpotbugsPlugins() tests
    // -------------------------------------------------------------------------

    void "getSpotbugsPlugins returns empty string when no plugins configured"() {
        given:
        Log log = Mock() {
            isDebugEnabled() >> false
        }
        Path tempOutputDir = Files.createTempDirectory("spotbugs-output")
        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))
        impl.spotbugsXmlOutputDirectory = tempOutputDir.toFile()

        when:
        String result = impl.getSpotbugsPlugins()

        then:
        result == ''

        cleanup:
        tempOutputDir.toFile().deleteDir()
    }

    void "getSpotbugsPlugins includes JAR from pluginList"() {
        given:
        Log log = Mock() {
            isDebugEnabled() >> true
        }
        Path tempOutputDir = Files.createTempDirectory("spotbugs-output-plugin")
        Path jarPath = Files.createTempFile("custom-plugin", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("findbugs.xml"))
            jos.write("<FindbugsPlugin></FindbugsPlugin>".bytes)
            jos.closeEntry()
        }

        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))
        impl.spotbugsXmlOutputDirectory = tempOutputDir.toFile()
        impl.pluginList = jarPath.toAbsolutePath().toString()

        when:
        String result = impl.getSpotbugsPlugins()

        then:
        result.contains(jarPath.toFile().name)

        cleanup:
        Files.deleteIfExists(jarPath)
        tempOutputDir.toFile().deleteDir()
    }

    void "getSpotbugsPlugins includes plugin JAR from non-spotbugs pluginArtifacts"() {
        given:
        Log log = Mock() {
            isDebugEnabled() >> true
        }
        Path tempOutputDir = Files.createTempDirectory("spotbugs-output-artifact")
        Path jarPath = Files.createTempFile("artifact-ext-plugin", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("findbugs.xml"))
            jos.write("<FindbugsPlugin></FindbugsPlugin>".bytes)
            jos.closeEntry()
        }

        Artifact artifact = Mock(Artifact)
        artifact.groupId >> "com.example"
        artifact.file >> jarPath.toFile()

        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))
        impl.spotbugsXmlOutputDirectory = tempOutputDir.toFile()
        impl.pluginArtifacts = [artifact]

        when:
        String result = impl.getSpotbugsPlugins()

        then:
        result.contains(jarPath.toFile().name)

        cleanup:
        Files.deleteIfExists(jarPath)
        tempOutputDir.toFile().deleteDir()
    }

    void "getSpotbugsPlugins skips pluginArtifacts from com.github.spotbugs group"() {
        given:
        Log log = Mock() {
            isDebugEnabled() >> true
        }
        Path tempOutputDir = Files.createTempDirectory("spotbugs-output-skip")
        Path jarPath = Files.createTempFile("spotbugs-core", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("findbugs.xml"))
            jos.write("<FindbugsPlugin></FindbugsPlugin>".bytes)
            jos.closeEntry()
        }

        Artifact artifact = Mock(Artifact)
        artifact.groupId >> "com.github.spotbugs"
        artifact.file >> jarPath.toFile()

        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))
        impl.spotbugsXmlOutputDirectory = tempOutputDir.toFile()
        impl.pluginArtifacts = [artifact]

        when:
        String result = impl.getSpotbugsPlugins()

        then:
        result == ''

        cleanup:
        Files.deleteIfExists(jarPath)
        tempOutputDir.toFile().deleteDir()
    }

    void "getSpotbugsPlugins skips non-spotbugs artifact without findbugs.xml"() {
        given:
        Log log = Mock() {
            isDebugEnabled() >> false
        }
        Path tempOutputDir = Files.createTempDirectory("spotbugs-output-no-fb")
        Path jarPath = Files.createTempFile("regular-lib", ".jar")
        new JarOutputStream(Files.newOutputStream(jarPath)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry("com/example/Lib.class"))
            jos.write([0xCA, 0xFE, 0xBA, 0xBE] as byte[])
            jos.closeEntry()
        }

        Artifact artifact = Mock(Artifact)
        artifact.groupId >> "com.example"
        artifact.file >> jarPath.toFile()

        SpotBugsPluginsTraitImpl impl = new SpotBugsPluginsTraitImpl("default", log, Mock(ResourceManager),
            Mock(org.eclipse.aether.RepositorySystem), Mock(org.apache.maven.repository.RepositorySystem), Mock(MavenSession))
        impl.spotbugsXmlOutputDirectory = tempOutputDir.toFile()
        impl.pluginArtifacts = [artifact]

        when:
        String result = impl.getSpotbugsPlugins()

        then:
        result == ''

        cleanup:
        Files.deleteIfExists(jarPath)
        tempOutputDir.toFile().deleteDir()
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
