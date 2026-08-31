/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package org.codehaus.mojo.spotbugs

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

import java.util.jar.JarEntry
import java.util.jar.JarFile

import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugin.MojoExecutionException
import org.codehaus.plexus.resource.ResourceManager
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResult
import org.xml.sax.SAXException

/**
 * SpotBugs plugin support for Mojos.
 */
@CompileStatic
trait SpotBugsPluginsTrait {

    // the trait needs certain objects to work, this need is expressed as abstract getters
    // classes implement them with implicitly generated property getters
    abstract RepositorySystem getRepositorySystem()
    abstract File getSpotbugsXmlOutputDirectory()
    abstract Log getLog()
    abstract ResourceManager getResourceManager()

    // TODO This has been fixed for years now, apply as noted...
    // properties in traits should be supported but don't compile currently:
    // https://issues.apache.org/jira/browse/GROOVY-7536
    // when fixed, should move pluginList and plugins properties here
    abstract String getPluginList()
    abstract List<PluginArtifact> getPlugins()
    abstract List<org.apache.maven.artifact.Artifact> getPluginArtifacts()
    abstract String getEffort()
    abstract MavenSession getSession()

    /**
     * Adds the specified plugins to spotbugs. The coreplugin is always added first.
     *
     */
    String getSpotbugsPlugins() {
        ResourceHelper resourceHelper = new ResourceHelper(log, new File(spotbugsXmlOutputDirectory, "spotbugs"), resourceManager)

        List<String> urlPlugins = new ArrayList<>()

        if (pluginList != null && !pluginList.isEmpty()) {
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

                    urlPlugins.add(resourceHelper.getResourceFile(pluginFileName).absolutePath)
                } catch (MalformedURLException e) {
                    throw new MojoExecutionException('The addin plugin has an invalid URL', e)
                }
            }
        }

        if (plugins != null && !plugins.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug('  Adding Plugins from a repository')
                log.debug("  Session is: ${session}")
            }

            plugins.each { PluginArtifact plugin ->

                if (log.isDebugEnabled()) {
                    log.debug("  Processing Plugin: ${plugin}")
                }

                Artifact pomArtifact = new DefaultArtifact(
                    plugin.groupId,
                    plugin.artifactId,
                    plugin.classifier,
                    plugin.type,
                    plugin.version
                )

                if (log.isDebugEnabled()) {
                    log.debug("  Added Artifact: ${pomArtifact}")
                }

                ArtifactRequest request = new ArtifactRequest(
                    pomArtifact,
                    session.getCurrentProject().getRemoteProjectRepositories(),
                    null
                )

                ArtifactResult result = this.repositorySystem.resolveArtifact(
                    session.getRepositorySession(),
                    request
                )

                urlPlugins.add(resourceHelper.getResourceFile(result.artifact.file.absolutePath).absolutePath)
            }
        }

        // Auto-detect SpotBugs extension plugins added as standard Maven <dependencies> to the plugin.
        // Any artifact on the plugin classpath (pluginArtifacts) that contains findbugs.xml
        // and is not part of the SpotBugs core (com.github.spotbugs group) is treated as a plugin extension.
        if (pluginArtifacts != null && !pluginArtifacts.isEmpty()) {
            log.debug('  Scanning plugin artifacts for SpotBugs extension plugins (added via <dependencies>)')

            // Collect file names already in the plugin list to avoid adding the same JAR twice
            // (e.g. when a plugin is declared both via <plugins> config and as a <dependency>).
            Set<String> addedFileNames = new HashSet<>()

            urlPlugins.each { String plugin ->
                addedFileNames.add(new File(plugin).name)
            }

            pluginArtifacts.each { org.apache.maven.artifact.Artifact artifact ->
                if ('com.github.spotbugs' != artifact.groupId && artifact.file != null && isSpotBugsPlugin(artifact.file)) {
                    String jarFileName = artifact.file.name
                    if (!addedFileNames.contains(jarFileName)) {
                        if (log.isDebugEnabled()) {
                            log.debug("  Auto-detected SpotBugs extension plugin from dependency: ${artifact}")
                        }
                        addedFileNames.add(jarFileName)
                        urlPlugins.add(resourceHelper.getResourceFile(artifact.file.absolutePath).absolutePath)
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
            new JarFile(file).withCloseable { JarFile jar ->
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

        Map<String, String> effectiveUrls = new HashMap<>(defaults)
        if (userPluginDocUrls != null) {
            effectiveUrls.putAll(userPluginDocUrls)
        }

        Map<String, String> bugTypeUrlMap = new HashMap<>()

        // Collect all candidate JAR files from pluginList and pluginArtifacts.
        // We read the JARs directly (without copying) since we only need their metadata.
        Set<File> pluginJars = new HashSet<>()

        if (pluginList != null && !pluginList.isEmpty()) {
            pluginList.split(SpotBugsInfo.COMMA).each { String path ->
                String trimmed = path.trim()
                if (!trimmed.isEmpty()) {
                    File jar = new File(trimmed)
                    if (jar.exists()) {
                        pluginJars.add(jar)
                    }
                }
            }
        }

        if (pluginArtifacts != null && !pluginArtifacts.isEmpty()) {
            pluginArtifacts.each { org.apache.maven.artifact.Artifact artifact ->
                if ('com.github.spotbugs' != artifact.groupId && artifact.file != null && artifact.file.exists() && artifact.file.name.endsWith('.jar')) {
                    pluginJars.add(artifact.file)
                }
            }
        }

        pluginJars.each { File pluginJar ->
            try {
                new JarFile(pluginJar).withCloseable { JarFile jar ->
                    JarEntry entry = jar.getEntry('findbugs.xml')
                    if (entry == null) {
                        return
                    }

                    GPathResult xml = new XmlSlurper().parse(jar.getInputStream(entry))
                    String pluginId = xml.@pluginid.text()
                    String urlTemplate = effectiveUrls.get(pluginId)
                    if (urlTemplate == null) {
                        return
                    }

                    xml.BugPattern.each { GPathResult bugPattern ->
                        String type = bugPattern.@type.text()
                        if (!type.isEmpty()) {
                            bugTypeUrlMap.put(type, urlTemplate.replace('{type}', type))
                        }
                    }
                }
            } catch (IOException | SAXException e) {
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
        String effortParameter
        if (effort == 'Max') {
            effortParameter = 'max'
        } else if (effort == 'Min') {
            effortParameter = 'min'
        } else {
            effortParameter = 'default'
        }

        if (log.isDebugEnabled()) {
            log.debug("effort is ${effort}")
            log.debug("effortParameter is ${effortParameter}")
        }

        return "-effort:${effortParameter}"
    }
}
