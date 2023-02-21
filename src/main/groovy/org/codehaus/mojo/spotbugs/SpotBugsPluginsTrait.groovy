package org.codehaus.mojo.spotbugs

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.repository.ArtifactRepository

import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.ProjectBuildingRequest
import org.apache.maven.plugin.MojoExecutionException

import org.apache.maven.repository.RepositorySystem

import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult
import org.codehaus.plexus.resource.ResourceManager

/**
 * SpotBugs plugin support for Mojos.
 */
trait SpotBugsPluginsTrait {

    // the trait needs certain objects to work, this need is expressed as abstract getters
    // classes implement them with implicitly generated property getters
    abstract ArtifactResolver getArtifactResolver()
    abstract RepositorySystem getFactory()
    abstract List getRemoteRepositories()
    abstract ArtifactRepository getLocalRepository()
    abstract File getSpotbugsXmlOutputDirectory()
    abstract Log getLog()
    abstract ResourceManager getResourceManager()

    // TODO This has been fixed for 2 years now, apply as noted...
    // properties in traits should be supported but don't compile currently:
    // https://issues.apache.org/jira/browse/GROOVY-7536
    // when fixed, should move pluginList and plugins properties here
    abstract String getPluginList()
    abstract PluginArtifact[] getPlugins()

    /**
     * Adds the specified plugins to spotbugs. The coreplugin is always added first.
     *
     */
    String getSpotbugsPlugins() {
        ResourceHelper resourceHelper = new ResourceHelper(log, spotbugsXmlOutputDirectory, resourceManager)

        URL[] pluginURL

        def urlPlugins = ""

        if (pluginList) {
            log.debug("  Adding Plugins ")
            String[] pluginJars = pluginList.split(SpotBugsInfo.COMMA)

            pluginJars.each() { pluginJar ->
                def pluginFileName = pluginJar.trim()

                if (!pluginFileName.endsWith(".jar")) {
                    throw new IllegalArgumentException("Plugin File is not a Jar file: " + pluginFileName)
                }

                try {
                    log.debug("  Processing Plugin: " + pluginFileName.toString())

                    urlPlugins += resourceHelper.getResourceFile(pluginFileName.toString()).absolutePath + ((pluginJar == pluginJars[pluginJars.size() - 1]) ? "" : File.pathSeparator)
                } catch (MalformedURLException e) {
                    throw new MojoExecutionException("The addin plugin has an invalid URL", e)
                }
            }
        }

        if (plugins) {
            log.debug("  Adding Plugins from a repository")

            if (urlPlugins.size() > 0) {
                urlPlugins += File.pathSeparator
            }

            Artifact pomArtifact

            ProjectBuildingRequest configuration = session.getProjectBuildingRequest();
            log.debug("  Session is: " + session.toString())
            configuration.setRemoteRepositories(this.remoteRepositories)
            configuration.setLocalRepository(this.localRepository)

            plugins.each() { plugin ->

                log.debug("  Processing Plugin: " + plugin.toString())
                log.debug("groupId is ${plugin['groupId']} ****** artifactId is ${plugin['artifactId']} ****** version is ${plugin['version']} ****** type is ${plugin['type']}")
                pomArtifact = this.factory.createArtifact(plugin['groupId'], plugin['artifactId'], plugin['version'], "", plugin['type'])
                log.debug("pomArtifact is ${pomArtifact} ****** groupId is ${plugin['groupId']} ****** artifactId is ${plugin['artifactId']} ****** version is ${plugin['version']} ****** type is ${plugin['type']}")

                pomArtifact = artifactResolver.resolveArtifact(configuration, pomArtifact).getArtifact()

                urlPlugins += resourceHelper.getResourceFile(pomArtifact.file.absolutePath).absolutePath + ((plugin == plugins[plugins.size() - 1]) ? "" : File.pathSeparator)
            }
        }

        log.debug("  Plugin list is: ${urlPlugins}")

        return urlPlugins
    }

    /**
     * Returns the effort parameter to use.
     *
     * @return A valid effort parameter.
     *
     */
    String getEffortParameter() {
        log.debug("effort is ${effort}")

        String effortParameter

        switch (effort) {
            case "Max":
                effortParameter = "max"; break

            case "Min":
                effortParameter = "min"; break

            default:
                effortParameter = "default"; break
        }

        log.debug("effortParameter is ${effortParameter}")

        return "-effort:" + effortParameter
    }

}
