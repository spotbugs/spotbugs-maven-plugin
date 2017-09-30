package org.codehaus.mojo.findbugs

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.artifact.resolver.ArtifactResolver
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.tools.SiteTool
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Execute
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.resource.ResourceManager
import org.codehaus.plexus.util.FileUtils

/**
 * Fail the build if there were any FindBugs violations in the source code.
 * An XML report is put out by default in the target directory with the errors.
 * To see more documentation about FindBugs' options, please see the <a href="http://spotbugs.readthedocs.io/en/latest/" class="externalLink">SpotBugs
Manual.</a>.
 *
 * @since 2.0
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 */

@Mojo( name = "check", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true, threadSafe = true )
@Execute( goal = "spotbugs")
class SpotbugsViolationCheckMojo extends AbstractMojo {

    /**
     * Location where generated html will be created.
     *
     */
    @Parameter( defaultValue = '${project.reporting.outputDirectory}', required = true )
    File outputDirectory

    /**
     * Turn on and off xml output of the Spotbugs report.
     *
     * @since 1.0.0
     */
    @Parameter( defaultValue = "false", property="spotbugs.xmlOutput", required = true )
    boolean xmlOutput

    /**
     * Specifies the directory where the xml output will be generated.
     *
     * @since 1.0.0
     */
    @Parameter( defaultValue = '${project.build.directory}', required = true )
    File xmlOutputDirectory

    /**
     * Location where generated html will be created.
     *
     */

    /**
     * This has been deprecated and is on by default.
     *
     * @since 1.2.0
     *
     */
    @Deprecated
    @Parameter( defaultValue = "true" )
    boolean spotbugsXmlOutput

    /**
     * Specifies the directory where the spotbugs native xml output will be generated.
     *
     * @since 1.2.0
     */
    @Parameter( defaultValue = '${project.build.directory}', required = true )
    File spotbugsXmlOutputDirectory

    /**
     * Doxia Site Renderer.
     */
    @Component( role = Renderer.class)
    Renderer siteRenderer

    /**
     * Directory containing the class files for FindBugs to analyze.
     */
    @Parameter( defaultValue = '${project.build.outputDirectory}', required = true )
    File classFilesDirectory

    /**
     * Directory containing the test class files for FindBugs to analyze.
     *
     */
    @Parameter( defaultValue = '${project.build.testOutputDirectory}', required = true )
    File testClassFilesDirectory

    /**
     * Location of the Xrefs to link to.
     *
     */
    @Parameter( defaultValue = '${project.reporting.outputDirectory}/xref' )
    File xrefLocation

    /**
     * Location of the Test Xrefs to link to.
     *
     */
    @Parameter( defaultValue = '${project.reporting.outputDirectory}/xref-test' )
    File xrefTestLocation

    /**
     * The directories containing the sources to be compiled.
     *
     */
    @Parameter( defaultValue = '${project.compileSourceRoots}', required = true, readonly = true )
    List compileSourceRoots

    /**
     * The directories containing the test-sources to be compiled.
     *
     * @since 2.0
     */
    @Parameter( defaultValue = '${project.testCompileSourceRoots}', required = true, readonly = true )
    List testSourceRoots

    /**
     * Run Spotbugs on the tests.
     *
     * @since 2.0
     */
    @Parameter( defaultValue = "false", property="spotbugs.includeTests" )
    boolean includeTests

    /**
     * List of artifacts this plugin depends on. Used for resolving the Spotbugs coreplugin.
     *
     */
    @Parameter( property="plugin.artifacts", required = true, readonly = true )
    List pluginArtifacts

    /**
     * The local repository, needed to download the coreplugin jar.
     *
     */
    @Parameter( property="localRepository", required = true, readonly = true )
    ArtifactRepository localRepository

    /**
     * Remote repositories which will be searched for the coreplugin jar.
     *
     */
    @Parameter( property="project.remoteArtifactRepositories", required = true, readonly = true )
    List remoteArtifactRepositories

    /**
     * Maven Project
     *
     */
    @Parameter( property="project", required = true, readonly = true )
    MavenProject project

    /**
     * Encoding used for xml files. Default value is UTF-8.
     *
     */
    @Parameter( defaultValue = "UTF-8", readonly = true )
    String xmlEncoding

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = '${project.build.sourceEncoding}', property="encoding" )
    String sourceEncoding

    /**
     * The file encoding to use when creating the HTML reports. If the property <code>project.reporting.outputEncoding</code>
     * is not set, the platform default encoding is used.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = '${project.reporting.outputEncoding}', property="outputEncoding" )
    String outputEncoding

    /**
     * Threshold of minimum bug severity to report. Valid values are High, Default, Low, Ignore, and Exp (for experimental).
     *
     */
    @Parameter( defaultValue = "Default", property="spotbugs.threshold" )
    String threshold

    /**
     * Artifact resolver, needed to download the coreplugin jar.
     */
    @Component( role = ArtifactResolver.class )
    ArtifactResolver artifactResolver

    /**
     * <p>
     * File name of the include filter. Only bugs in matching the filters are reported.
     * </p>
     *
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     * </p>
     *
     * @since 1.0-beta-1
     */
    @Parameter( property="spotbugs.includeFilterFile" )
    String includeFilterFile

    /**
     * <p>
     * File name of the exclude filter. Bugs matching the filters are not reported.
     * </p>
     *
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     * </p>
     *
     * @since 1.0-beta-1
     */
    @Parameter( property="spotbugs.excludeFilterFile" )
    String excludeFilterFile

    /**
     * <p>
     * File names of the baseline files. Bugs found in the baseline files won't be reported.
     * </p>
     *
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     * </p>
     *
     * This is a comma-delimited list.
     *
     * @since 2.4.1
     */
    @Parameter( property="spotbugs.excludeBugsFile" )
    String excludeBugsFile

    /**
     * Effort of the bug finders. Valid values are Min, Default and Max.
     *
     * @since 1.0-beta-1
     */
    @Parameter( defaultValue = "Default", property="spotbugs.effort" )
    String effort

    /**
     * turn on Spotbugs debugging
     *
     */
    @Parameter( defaultValue = "false", property="spotbugs.debug" )
    Boolean debug

    /**
     * Relaxed reporting mode. For many detectors, this option suppresses the heuristics used to avoid reporting false
     * positives.
     *
     * @since 1.1
     */
    @Parameter( defaultValue = "false", property="spotbugs.relaxed" )
    Boolean relaxed

    /**
     * The visitor list to run. This is a comma-delimited list.
     *
     * @since 1.0-beta-1
     */
    @Parameter( property="spotbugs.visitors" )
    String visitors

    /**
     * The visitor list to omit. This is a comma-delimited list.
     *
     * @since 1.0-beta-1
     */
    @Parameter( property="spotbugs.omitVisitors" )
    String omitVisitors

    /**
     * <p>
     * The plugin list to include in the report. This is a comma-delimited list.
     * </p>
     *
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a plugin file.
     * </p>
     *
     * @since 1.0-beta-1
     */
    @Parameter( property="spotbugs.pluginList" )
    String pluginList

    /**
     * Restrict analysis to the given comma-separated list of classes and packages.
     *
     * @since 1.1
     */
    @Parameter( property="spotbugs.onlyAnalyze" )
    String onlyAnalyze

    /**
     * This option enables or disables scanning of nested jar and zip files found
     *  in the list of files and directories to be analyzed.
     *
     * @since 2.3.2
     */
    @Parameter( property="spotbugs.nested", defaultValue = "false" )
    Boolean nested

    /**
     * Prints a trace of detectors run and classes analyzed to standard output.
     * Useful for troubleshooting unexpected analysis failures.
     *
     * @since 2.3.2
     */
    @Parameter( property="spotbugs.trace", defaultValue = "false" )
    Boolean trace

    /**
     * Maximum bug ranking to record.
     *
     * @since 2.4.1
     */
    @Parameter( property="spotbugs.maxRank" )
    int maxRank

    /**
     * Skip entire check.
     *
     * @since 1.1
     */
    @Parameter( property="spotbugs.skip", defaultValue = "false" )
    boolean skip

    /**
     * @since 2.0
     */
    @Component( role = ResourceManager.class)
    ResourceManager resourceManager

    /**
     * SiteTool.
     *
     * @since 2.1-SNAPSHOT
     */
    @Component( role = SiteTool.class)
    SiteTool siteTool

    /**
     * Fail the build on an error.
     *
     * @since 2.0
     */
    @Parameter( property="spotbugs.failOnError", defaultValue = "true" )
    boolean failOnError

    /**
     * Fork a VM for FindBugs analysis.  This will allow you to set timeouts and heap size
     *
     * @since 2.3.2
     */
    @Parameter( property="spotbugs.fork", defaultValue = "true" )
    boolean fork

    /**
     * Maximum Java heap size in megabytes  (default=512).
     * This only works if the <b>fork</b> parameter is set <b>true</b>.
     *
     * @since 2.2
     */
    @Parameter( property="spotbugs.maxHeap", defaultValue = "512" )
    int maxHeap

    /**
     * Specifies the amount of time, in milliseconds, that FindBugs may run before
     *  it is assumed to be hung and is terminated.
     * The default is 600,000 milliseconds, which is ten minutes.
     * This only works if the <b>fork</b> parameter is set <b>true</b>.
     *
     * @since 2.2
     */
    @Parameter( property="spotbugs.timeout", defaultValue = "600000" )
    int timeout

    /**
     * <p>
     * the arguments to pass to the forked VM (ignored if fork is disabled).
     * </p>
     *
     * @since 2.4.1
     */
    @Parameter( property="spotbugs.jvmArgs" )
    String jvmArgs


	int bugCount

	int errorCount


	void execute() {
		Locale locale = Locale.getDefault()
		List sourceFiles

		log.debug("Executing spotbugs:check")

		if ( this.classFilesDirectory.exists() && this.classFilesDirectory.isDirectory() ) {
			sourceFiles = FileUtils.getFiles(classFilesDirectory, FindBugsInfo.JAVA_REGEX_PATTERN, null)
		}

		if ( !skip && sourceFiles ) {

			// this goes

			log.debug("Here goes...............Executing spotbugs:check")

			if (!spotbugsXmlOutputDirectory.exists()) {
				if ( !spotbugsXmlOutputDirectory.mkdirs() ) {
                    throw new MojoExecutionException("Cannot create xml output directory")
				}
			}


			File outputFile = new File("${spotbugsXmlOutputDirectory}/spotbugsXml.xml")

			if (outputFile.exists()) {

				def path = new XmlSlurper().parse(outputFile)

				def allNodes = path.depthFirst().collect { it }

				bugCount = allNodes.findAll {it.name() == 'BugInstance'}.size()
				log.info("BugInstance size is ${bugCount}")

				errorCount = allNodes.findAll {it.name() == 'Error'}.size()
				log.info("Error size is ${errorCount}")

                def xml = new XmlParser().parse(outputFile)
                def bugs = xml.BugInstance
                def total = bugs.size()

                if (total <= 0) {
                    log.info('No errors/warnings found')
                    return
                }

                log.info('Total bugs: ' + total)
                for (i in 0..total-1) {
                    def bug = bugs[i]
                    log.info( bug.LongMessage.text() + FindBugsInfo.BLANK + bug.SourceLine.'@classname' + FindBugsInfo.BLANK + bug.SourceLine.Message.text() + FindBugsInfo.BLANK + bug.'@type')
                }


                log.info('\n\n\nTo see bug detail using the Spotbugs GUI, use the following command "mvn spotbugs:gui"\n\n\n')


                if ( (bugCount || errorCount) && failOnError ) {
                    throw new MojoExecutionException("failed with ${bugCount} bugs and ${errorCount} errors ")
                }
            }
		}
		else {
			log.debug("Nothing for FindBugs to do here.")
		}
	}

}
