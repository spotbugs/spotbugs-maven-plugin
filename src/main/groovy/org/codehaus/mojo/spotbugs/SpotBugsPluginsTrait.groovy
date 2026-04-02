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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.XmlSlurper

import java.util.jar.JarEntry
import java.util.jar.JarFile

import org.apache.maven.RepositoryUtils
import org.apache.maven.artifact.Artifact
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugin.MojoExecutionException
import org.codehaus.plexus.resource.ResourceManager
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResult

/**
 * SpotBugs plugin support for Mojos.
 */
@CompileStatic
trait SpotBugsPluginsTrait {

    // the trait needs certain objects to work, this need is expressed as abstract getters
    // classes implement them with implicitly generated property getters
    abstract org.eclipse.aether.RepositorySystem getRepositorySystem()
    abstract org.apache.maven.repository.RepositorySystem getFactory()
    abstract File getSpotbugsXmlOutputDirectory()
    abstract Log getLog()
    abstract ResourceManager getResourceManager()

    // TODO This has been fixed for years now, apply as noted...
    // properties in traits should be supported but don't compile currently:
    // https://issues.apache.org/jira/browse/GROOVY-7536
    // when fixed, should move pluginList and plugins properties here
    abstract String getPluginList()
    abstract List<PluginArtifact> getPlugins()
    abstract List<Artifact> getPluginArtifacts()
    abstract String getEffort()
    abstract MavenSession getSession()

    /**
     * Adds the specified plugins to spotbugs. The coreplugin is always added first.
     *
     */
    String getSpotbugsPlugins() {
        ResourceHelper resourceHelper = new ResourceHelper(log, new File(spotbugsXmlOutputDirectory, "spotbugs"), resourceManager)

        List<String> urlPlugins = []

        if (pluginList) {
            log.debug('  Adding Plugins ')

            pluginList.split(SpotBugsInfo.COMMA).each { String pluginJar ->
                String pluginFileName = pluginJar.trim()

                if (!pluginFileName.endsWith('.jar')) {
                    throw new MojoExecutionException("Plugin File is not a Jar file: ${pluginFileName}")
                }

                try {
                    if (log.isDebugEnabled()) {
                        log.debug("  Processing Plugin: ${pluginFileName}")
                    }

                    urlPlugins << resourceHelper.getResourceFile(pluginFileName).absolutePath
                } catch (MalformedURLException e) {
                    throw new MojoExecutionException('The addin plugin has an invalid URL', e)
                }
            }
        }

        if (plugins) {
            if (log.isDebugEnabled()) {
                log.debug('  Adding Plugins from a repository')
                log.debug("  Session is: ${session}")
            }

            plugins.each { PluginArtifact plugin ->

                if (log.isDebugEnabled()) {
                    log.debug("  Processing Plugin: ${plugin}")
                }

                Artifact pomArtifact = plugin.classifier == null ?
                    this.factory.createArtifact(plugin.groupId, plugin.artifactId, plugin.version, "", plugin.type) :
                    this.factory.createArtifactWithClassifier(plugin.groupId, plugin.artifactId, plugin.version, plugin.type, plugin.classifier)

                if (log.isDebugEnabled()) {
                    log.debug("  Added Artifact: ${pomArtifact}")
                }

                ArtifactRequest request = new ArtifactRequest(RepositoryUtils.toArtifact(pomArtifact), session.getCurrentProject().getRemoteProjectRepositories(), null)
                ArtifactResult result = this.repositorySystem.resolveArtifact(session.getRepositorySession(), request)

                pomArtifact.setFile(result.getArtifact().getFile())

                urlPlugins << resourceHelper.getResourceFile(pomArtifact.file.absolutePath).absolutePath
            }
        }

        // Auto-detect SpotBugs extension plugins added as standard Maven <dependencies> to the plugin.
        // Any artifact on the plugin classpath (pluginArtifacts) that contains findbugs.xml
        // and is not part of the SpotBugs core (com.github.spotbugs group) is treated as a plugin extension.
        if (pluginArtifacts) {
            if (log.isDebugEnabled()) {
                log.debug('  Scanning plugin artifacts for SpotBugs extension plugins (added via <dependencies>)')
            }

            // Collect file names already in the plugin list to avoid adding the same JAR twice
            // (e.g. when a plugin is declared both via <plugins> config and as a <dependency>).
            Set<String> addedFileNames = urlPlugins.collect { new File(it).name } as Set

            pluginArtifacts.each { Artifact artifact ->
                if ('com.github.spotbugs' != artifact.groupId && artifact.file != null && isSpotBugsPlugin(artifact.file)) {
                    String jarFileName = artifact.file.name
                    if (!addedFileNames.contains(jarFileName)) {
                        if (log.isDebugEnabled()) {
                            log.debug("  Auto-detected SpotBugs extension plugin from dependency: ${artifact}")
                        }
                        addedFileNames << jarFileName
                        urlPlugins << resourceHelper.getResourceFile(artifact.file.absolutePath).absolutePath
                    }
                }
            }
        }

        String pluginListStr = urlPlugins.join(File.pathSeparator)

        if (log.isDebugEnabled()) {
            log.debug("  Plugin list is: ${pluginListStr}")
        }

        return pluginListStr
    }

    /**
     * Determines whether the given file is a SpotBugs extension plugin by checking
     * if it is a JAR containing {@code findbugs.xml} at the root.
     *
     * @param file the artifact file to inspect
     * @return {@code true} if the file is a SpotBugs plugin JAR, {@code false} otherwise
     */
    boolean isSpotBugsPlugin(File file) {
        if (file == null || !file.exists() || !file.name.endsWith('.jar')) {
            return false
        }
        try {
            new JarFile(file).withCloseable { jar ->
                return jar.getEntry('findbugs.xml') != null
            }
        } catch (IOException ignored) {
            return false
        }
    }

    /**
     * Builds a mapping from bug type codes to their documentation URLs by reading
     * the {@code findbugs.xml} descriptor from each resolved SpotBugs plugin JAR.
     * <p>
     * The method inspects JARs from two sources:
     * <ol>
     *   <li>Plugin JARs listed in {@code pluginList} (comma-separated file paths).</li>
     *   <li>Plugin JARs discovered automatically from {@code pluginArtifacts} (Maven dependencies).</li>
     * </ol>
     * Built-in documentation URL templates are provided for the following well-known plugins:
     * <ul>
     *   <li>{@code com.mebigfatguy.fbcontrib} &rarr; fb-contrib / sb-contrib</li>
     *   <li>{@code com.h3xstream.findsecbugs} &rarr; Find Security Bugs</li>
     * </ul>
     * User-supplied entries in {@code userPluginDocUrls} override the built-in defaults.
     * URL templates may contain the placeholder {@code {type}} which will be replaced with
     * the bug type code (e.g. {@code https://example.com/bugs.html#{type}}).
     *
     * @param userPluginDocUrls optional user-configured map of plugin IDs to URL templates
     * @return a map from bug type code to fully-resolved documentation URL
     */
    @CompileDynamic
    Map<String, String> buildBugTypeUrlMap(Map<String, String> userPluginDocUrls) {
        Map<String, String> defaults = [
            'com.mebigfatguy.fbcontrib'   : 'https://fb-contrib.sourceforge.net/bugdescriptions.html#{type}',
            'com.h3xstream.findsecbugs'   : 'https://find-sec-bugs.github.io/bugs.htm#{type}',
        ]

        Map<String, String> effectiveUrls = defaults + (userPluginDocUrls ?: [:])
        Map<String, String> bugTypeUrlMap = [:]

        // Collect all candidate JAR files from pluginList and pluginArtifacts.
        // We read the JARs directly (without copying) since we only need their metadata.
        Set<File> pluginJars = []

        if (pluginList) {
            pluginList.split(SpotBugsInfo.COMMA).each { String path ->
                String trimmed = path?.trim()
                if (trimmed) {
                    File jar = new File(trimmed)
                    if (jar.exists()) pluginJars << jar
                }
            }
        }

        if (pluginArtifacts) {
            pluginArtifacts.each { Artifact artifact ->
                if ('com.github.spotbugs' != artifact.groupId && artifact.file != null && artifact.file.exists()) {
                    pluginJars << artifact.file
                }
            }
        }

        pluginJars.each { File pluginJar ->
            try {
                new JarFile(pluginJar).withCloseable { JarFile jar ->
                    JarEntry entry = jar.getEntry('findbugs.xml')
                    if (!entry) return

                    def xml = new XmlSlurper().parse(jar.getInputStream(entry))
                    String pluginId = xml.@pluginid.text()
                    String urlTemplate = effectiveUrls[pluginId]
                    if (!urlTemplate) return

                    xml.BugPattern.each { bugPattern ->
                        String type = bugPattern.@type.text()
                        if (type) {
                            bugTypeUrlMap[type] = urlTemplate.replace('{type}', type)
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to read SpotBugs plugin JAR for URL mapping: ${pluginJar}: ${e.message}")
            }
        }

        return bugTypeUrlMap
    }

    /**
     * Returns the effort parameter to use.
     *
     * @return A valid effort parameter.
     *
     */
    String getEffortParameter() {
        String effortParameter = (effort == 'Max') ? 'max' : (effort == 'Min') ? 'min' : 'default'

        if (log.isDebugEnabled()) {
            log.debug("effort is ${effort}")
            log.debug("effortParameter is ${effortParameter}")
        }

        return "-effort:${effortParameter}"
    }
}
