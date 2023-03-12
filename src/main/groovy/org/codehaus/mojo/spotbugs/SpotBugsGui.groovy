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

import groovy.ant.AntBuilder

import org.apache.maven.artifact.repository.ArtifactRepository

import org.apache.maven.execution.MavenSession

import org.apache.maven.plugin.AbstractMojo

import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope

import org.apache.maven.project.MavenProject

import org.apache.maven.repository.RepositorySystem

import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver

import org.codehaus.plexus.resource.ResourceManager

/**
 * Launch the Spotbugs GUI.
 * It will use all the parameters in the POM fle.
 *
 * @since 2.0
 *
 * @description Launch the Spotbugs GUI using the parameters in the POM fle.
 */
@Mojo( name = "gui", requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true )
class SpotBugsGui extends AbstractMojo implements SpotBugsPluginsTrait {

    /**
     * locale to use for Resource bundle.
     */
    static Locale locale = Locale.ENGLISH

    /**
     * Directory containing the class files for Spotbugs to analyze.
     */
    @Parameter( defaultValue = '${project.build.outputDirectory}', required = true )
    File classFilesDirectory

    /**
     * Turn on Spotbugs debugging.
     *
     */
    @Parameter( defaultValue = "false", property="spotbugs.debug" )
    Boolean debug

    /**
     * List of artifacts this plugin depends on. Used for resolving the Spotbugs core plugin.
     *
     */
    @Parameter( property="plugin.artifacts", required = true, readonly = true )
    List pluginArtifacts

    /**
     * Effort of the bug finders. Valid values are Min, Default and Max.
     *
     */
    @Parameter( defaultValue = "Default", property="spotbugs.effort" )
    String effort

    /**
     * The plugin list to include in the report. This is a SpotbugsInfo.COMMA-delimited list.
     *
     */
    @Parameter( property="spotbugs.pluginList" )
    String pluginList

    /**
     * <p>
     * Collection of PluginArtifact to work on. (PluginArtifact contains groupId, artifactId, version, type.)
     * See <a href="./usage.html#Using Detectors from a Repository">Usage</a> for details.
     * </p>
     */
    @Parameter
    PluginArtifact[] plugins

    /**
     * Artifact resolver, needed to download the coreplugin jar.
     */
    @Component(role = ArtifactResolver.class)
    ArtifactResolver artifactResolver

    /**
     * Used to look up Artifacts in the remote repository.
     *
     */
    @Component(role = RepositorySystem.class)
    RepositorySystem factory

    /**
     * List of Remote Repositories used by the resolver.
     *
     */
    @Parameter(property = "project.remoteArtifactRepositories", required = true, readonly = true)
    List remoteRepositories

    /**
     * The local repository, needed to download the coreplugin jar.
     *
     */
    @Parameter(property = "localRepository", required = true, readonly = true)
    ArtifactRepository localRepository

    /**
     * Maven Session.
     */
    @Parameter (defaultValue = '${session}', required = true, readonly = true)
    MavenSession session;

    /**
     * Maven Project.
     *
     */
    @Parameter( property="project", required = true, readonly = true )
    MavenProject project

    /**
     * Resource bundle for a specific locale.
     *
     */
    @Parameter( readonly = true )
    ResourceBundle bundle

    /**
     * Specifies the directory where the Spotbugs native xml output will be generated.
     *
     */
    @Parameter( defaultValue = '${project.build.directory}', required = true )
    File spotbugsXmlOutputDirectory

    /**
     * Set the name of the output XML file produced
     *
     * @since 3.1.12.2
     */
    @Parameter(property = "spotbugs.outputXmlFilename", defaultValue = "spotbugsXml.xml")
    String spotbugsXmlOutputFilename

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used. <strong>Note:</strong> This parameter always overrides the
     * property <code>charset</code> from Checkstyle's <code>TreeWalker</code> module.
     *
     * @since 2.2
     */
    @Parameter( property="encoding", defaultValue = '${project.build.sourceEncoding}' )
    String encoding

    /**
     * Maximum Java heap size in megabytes  (default=512).
     *
     * @since 2.2
     */
    @Parameter( property="spotbugs.maxHeap", defaultValue = "512" )
    int maxHeap

    /**
     * Resource Manager.
     *
     * @since 2.0
     */
    @Component(role = ResourceManager.class)
    ResourceManager resourceManager

    @Override
    void execute() {

        def ant = new AntBuilder()

        def auxClasspathElements = project.compileClasspathElements

        if ( debug ) {
            log.debug("  Plugin Artifacts to be added ->" + pluginArtifacts.toString())
        }

        ant.project.setProperty('basedir', spotbugsXmlOutputDirectory.getAbsolutePath())
        ant.project.setProperty('verbose', "true")

        ant.java(classname: "edu.umd.cs.findbugs.LaunchAppropriateUI", fork: "true", failonerror: "true", clonevm: "true", maxmemory: "${maxHeap}m") {

            def effectiveEncoding = System.getProperty( "file.encoding", "UTF-8" )

            if ( encoding ) {
                effectiveEncoding = encoding
            }

            log.info("File Encoding is " + effectiveEncoding)

            sysproperty(key: "file.encoding" , value: effectiveEncoding)

            // spotbugs assumes that multiple arguments (because of options) means text mode, so need to request gui explicitly
            jvmarg(value: "-Dfindbugs.launchUI=gui2")

            // options must be added before the spotbugsXml path
            def spotbugsArgs = new ArrayList<String>()

            spotbugsArgs << getEffortParameter()

            if (pluginList || plugins) {
                spotbugsArgs << "-pluginList"
                spotbugsArgs << getSpotbugsPlugins()
            }
            spotbugsArgs.each { spotbugsArg ->
                log.debug("Spotbugs arg is ${spotbugsArg}")
                arg(value: spotbugsArg)
            }

            def spotbugsXmlName = spotbugsXmlOutputDirectory.toString() + "/" + spotbugsXmlOutputFilename
            def spotbugsXml = new File(spotbugsXmlName)

            if ( spotbugsXml.exists() ) {
                log.debug("  Found an SpotBugs XML at ->" + spotbugsXml.toString())
                arg(value: spotbugsXml)
            }

            classpath() {

                pluginArtifacts.each() { pluginArtifact ->
                    if ( debug ) {
                        log.debug("  Trying to Add to pluginArtifact ->" + pluginArtifact.file.toString())
                    }

                    pathelement(location: pluginArtifact.file)
                }
            }
        }
    }

}
