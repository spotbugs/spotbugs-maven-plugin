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
package org.codehaus.mojo.spotbugs

import org.codehaus.mojo.spotbugs.SpotBugsGui
import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject
import org.apache.maven.plugin.logging.Log
import org.codehaus.plexus.resource.ResourceManager
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.charset.StandardCharsets

// TODO Disabled until we get rid of groovy-ant as we need headless off here and groovy-ant will fail if we do that.
@Ignore
class SpotBugsGuiTest extends Specification {

    void "execute sets up AntBuilder and logs encoding"() {
        given:
        System.setProperty("java.awt.headless", "true")
        Log log = Mock(Log)
        MavenProject project = Mock(MavenProject)
        project.getCompileClasspathElements() >> ["foo.jar"]
        MavenSession session = Mock(MavenSession)
        session.getCurrentProject() >> project
        ResourceManager resourceManager = Mock(ResourceManager)
        File outputDir = File.createTempDir()
        File classFilesDir = File.createTempDir()
        List<PluginArtifact> pluginArtifacts = []
        SpotBugsGui gui = new SpotBugsGui()
        gui.metaClass.log = log
        gui.session = session
        gui.resourceManager = resourceManager
        gui.spotbugsXmlOutputDirectory = outputDir
        gui.spotbugsXmlOutputFilename = "spotbugsXml.xml"
        gui.classFilesDirectory = classFilesDir
        gui.pluginArtifacts = pluginArtifacts
        gui.debug = true
        gui.encoding = StandardCharsets.UTF_8.name()
        gui.effort = "Default"
        gui.maxHeap = 256

        when:
        gui.execute()

        then:
        1 * log.info({ it.contains("File Encoding is") })
        _ * log.debug(_)

        cleanup:
        outputDir.deleteDir()
        classFilesDir.deleteDir()
    }

    void "execute uses default encoding if not set"() {
        given:
        System.setProperty("java.awt.headless", "true")
        Log log = Mock(Log)
        MavenProject project = Mock(MavenProject)
        project.getCompileClasspathElements() >> ["foo.jar"]
        MavenSession session = Mock(MavenSession)
        session.getCurrentProject() >> project
        ResourceManager resourceManager = Mock(ResourceManager)
        File outputDir = File.createTempDir()
        File classFilesDir = File.createTempDir()
        List<PluginArtifact> pluginArtifacts = []
        SpotBugsGui gui = new SpotBugsGui()
        gui.metaClass.log = log
        gui.session = session
        gui.resourceManager = resourceManager
        gui.spotbugsXmlOutputDirectory = outputDir
        gui.spotbugsXmlOutputFilename = "spotbugsXml.xml"
        gui.classFilesDirectory = classFilesDir
        gui.pluginArtifacts = pluginArtifacts
        gui.debug = false
        gui.encoding = null
        gui.effort = "Default"
        gui.maxHeap = 256

        when:
        gui.execute()

        then:
        1 * log.info({ it.contains("File Encoding is") })

        cleanup:
        outputDir.deleteDir()
        classFilesDir.deleteDir()
    }

}
