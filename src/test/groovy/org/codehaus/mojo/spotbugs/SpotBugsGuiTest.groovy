/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package org.codehaus.mojo.spotbugs

import org.codehaus.mojo.spotbugs.SpotBugsGui
import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject
import org.apache.maven.plugin.logging.Log
import org.codehaus.plexus.resource.ResourceManager
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class SpotBugsGuiTest extends Specification {

    void "execute logs encoding and skips launch in headless mode when explicit encoding is set"() {
        given:
        System.setProperty("java.awt.headless", "true")
        Log log = Mock(Log) {
            isInfoEnabled() >> true
        }
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
        1 * log.warn('Skipping SpotBugs GUI launch in headless environment')

        cleanup:
        outputDir.deleteDir()
        classFilesDir.deleteDir()
    }

    void "execute uses default encoding and skips launch in headless mode when encoding is not set"() {
        given:
        System.setProperty("java.awt.headless", "true")
        Log log = Mock(Log) {
            isInfoEnabled() >> true
        }
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
        1 * log.warn('Skipping SpotBugs GUI launch in headless environment')

        cleanup:
        outputDir.deleteDir()
        classFilesDir.deleteDir()
    }

}
