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

import groovy.ant.AntBuilder
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChild
import groovy.xml.slurpersupport.NodeChildren
import groovy.xml.StreamingMarkupBuilder

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate
import java.util.jar.JarFile
import java.util.stream.Collectors

import javax.inject.Inject
import org.apache.maven.artifact.Artifact
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.ReportPlugin
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.reporting.AbstractMavenReport
import org.apache.maven.reporting.MavenReport
import org.apache.maven.settings.Settings
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest
import org.apache.maven.settings.crypto.SettingsDecrypter
import org.apache.maven.toolchain.ToolchainManager
import org.codehaus.plexus.resource.ResourceManager
import org.codehaus.plexus.resource.loader.FileResourceLoader

/**
 * Generates a SpotBugs Report when the site plugin is run.
 * The HTML report is generated for site commands only.
 */
@Mojo(name = 'spotbugs', requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true, threadSafe = true)
class SpotBugsMojo extends AbstractMavenReport implements SpotBugsPluginsTrait {

    /** Location where generated html will be created allowed to be not read only as defined in AbstractMavenParent. */
    @Parameter(defaultValue = '${project.reporting.outputDirectory}', required = true)
    File outputDirectory

    /**
     * Turn on and off xml output of the Spotbugs report.
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.xmlOutput', required = true)
    boolean xmlOutput

    /**
     * Output empty warning file if no classes are specified.
     *
     * @since 4.8.3.0
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.noClassOk', required = true)
    boolean noClassOk

    /**
     * Turn on and off HTML output of the Spotbugs report.
     *
     * @since 4.7.3.1
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.htmlOutput', required = true)
    boolean htmlOutput

    /**
     * Turn on and off SARIF output of the Spotbugs report.
     * SARIF is a JSON format standardize for all code scanning tools.
     * https://docs.github.com/en/code-security/secure-coding/integrating-with-code-scanning/sarif-support-for-code-scanning
     *
     * @since 4.3.1
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.sarifOutput', required = true)
    boolean sarifOutput

    /**
     * Sarif full Path used with sarif.
     *
     * @since 4.3.1
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.sarifFullPath', required = true)
    boolean sarifFullPath

    /**
     * Specifies the directory where the sarif output will be generated.
     *
     * @since 4.7.2.2
     */
    @Parameter(defaultValue = '${project.build.directory}', property = 'spotbugs.sarifOutputDirectory', required = true)
    File sarifOutputDirectory

    /**
     * Set the name of the output SARIF file produced.
     *
     * @since 4.7.2.2
     */
    @Parameter(defaultValue = 'spotbugsSarif.json', property = 'spotbugs.sarifOutputFilename', required = true)
    String sarifOutputFilename

    /**
     * Specifies the directory where the xml output will be generated.
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = '${project.build.directory}', required = true)
    File xmlOutputDirectory

    /**
     * Specifies the directory where the Spotbugs native xml output will be generated.
     *
     * @since 1.2.0
     */
    @Parameter(defaultValue = '${project.build.directory}', required = true)
    File spotbugsXmlOutputDirectory

    /**
     * Set the name of the output XML file produced
     *
     * @since 3.1.12.2
     */
    @Parameter(defaultValue = 'spotbugsXml.xml', property = 'spotbugs.outputXmlFilename')
    String spotbugsXmlOutputFilename

    /** Directory containing the class files for Spotbugs to analyze. */
    @Parameter(defaultValue = '${project.build.outputDirectory}', required = true)
    File classFilesDirectory

    /** Directory containing the test class files for Spotbugs to analyze. */
    @Parameter(defaultValue = '${project.build.testOutputDirectory}', required = true)
    File testClassFilesDirectory

    /** Location of the Xrefs to link to. */
    @Parameter(defaultValue = '${project.reporting.outputDirectory}/xref')
    File xrefLocation

    /** Location of the Test Xrefs to link to. */
    @Parameter(defaultValue = '${project.reporting.outputDirectory}/xref-test')
    File xrefTestLocation

    /**
     * Run Spotbugs on the tests.
     *
     * @since 2.0
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.includeTests')
    boolean includeTests

    /** Run Spotbugs with -sourcepath parameter populated with the known source roots. */
    @Parameter(defaultValue = 'false', property = 'spotbugs.addSourceDirs')
    boolean addSourceDirs

    /** List of artifacts this plugin depends on. Used for resolving the Spotbugs core plugin. */
    @Parameter(property = 'plugin.artifacts', readonly = true, required = true)
    List<Artifact> pluginArtifacts

    /** Maven Session. */
    @Parameter (defaultValue = '${session}', readonly = true, required = true)
    MavenSession session

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used.
     *
     * @since 2.2
     */
    @Parameter(defaultValue = '${project.build.sourceEncoding}', property = 'encoding')
    String sourceEncoding

    /**
     * The file encoding to use when creating the HTML reports. If the property <code>project.reporting.outputEncoding</code>
     * is not set, utf-8 is used.
     *
     * @since 2.2
     */
    @Parameter(defaultValue = '${project.reporting.outputEncoding}', property = 'outputEncoding')
    String outputEncoding

    /** Threshold of minimum bug severity to report. Valid values are High, Default, Low, Ignore, and Exp (for experimental). */
    @Parameter(defaultValue = 'Default', property = 'spotbugs.threshold')
    String threshold

    /** Artifact resolver, needed to download the plugin jars. */
    @Inject
    org.eclipse.aether.RepositorySystem repositorySystem

    /** Used to look up Artifacts in the remote repository. */
    @Inject
    org.apache.maven.repository.RepositorySystem factory

    /** Toolchain manager used to retrieve the JDK toolchain. */
    @Inject
    ToolchainManager toolchainManager

    /**
     * File name of the include filter. Only bugs in matching the filters are reported.
     * <p>
     * Potential values are a filesystem path, a URL, a classpath resource, or
     * a Maven artifact resource in the format
     * <code>mvn:groupId:artifactId:version[:type[:classifier]]!/path/in/archive</code>.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     * It supports multiple files separated by a comma
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = 'spotbugs.includeFilterFile')
    String includeFilterFile

    /**
     * File name for include filter files. Only bugs in matching the filters are reported.
     * <p>
     * Potential values are a filesystem path, a URL, a classpath resource, or
     * a Maven artifact resource in the format
     * <code>mvn:groupId:artifactId:version[:type[:classifier]]!/path/in/archive</code>.
     * <p>
     * This is an alternative to <code>&lt;includeFilterFile&gt;</code> which allows multiple
     * files to be specified as separate elements in a pom.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     *
     * @since 4.7.1.0
     */
    @Parameter(property = 'spotbugs.includeFilterFiles')
    List<String> includeFilterFiles

    /**
     * File name of the exclude filter. Bugs matching the filters are not reported.
     * <p>
     * Potential values are a filesystem path, a URL, a classpath resource, or
     * a Maven artifact resource in the format
     * <code>mvn:groupId:artifactId:version[:type[:classifier]]!/path/in/archive</code>.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     * It supports multiple files separated by a comma
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = 'spotbugs.excludeFilterFile')
    String excludeFilterFile

    /**
     * File name for exclude filter files. Bugs matching the filters are not reported.
     * <p>
     * This is an alternative to <code>&lt;excludeFilterFile&gt;</code> which allows multiple
     * files to be specified as separate elements in a pom.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     *
     * @since 4.7.1.0
     */
    @Parameter(property = 'spotbugs.excludeFilterFiles')
    List<String> excludeFilterFiles

    /**
     * File names of the baseline files. Bugs found in the baseline files won't be reported.
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     * <p>
     * This is a comma-delimited list.
     *
     * @since 2.4.1
     */
    @Parameter(property = 'spotbugs.excludeBugsFile')
    String excludeBugsFile

    /**
     * File names of the baseline files. Bugs found in the baseline files won't be reported.
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * <p>
     * This is an alternative to <code>&lt;excludeBugsFile&gt;</code> which allows multiple
     * files to be specified as separate elements in a pom.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a filter file.
     *
     * @since 4.7.1.0
     */
    @Parameter(property = 'spotbugs.excludeBugsFiles')
    List<String> excludeBugsFiles

    /**
     * Effort of the bug finders. Valid values are Min, Default and Max.
     *
     * @since 1.0-beta-1
     */
    @Parameter(defaultValue = 'Default', property = 'spotbugs.effort')
    String effort

    /** Turn on Spotbugs debugging. */
    @Parameter(defaultValue = 'false', property = 'spotbugs.debug')
    boolean debug

    /**
     * Relaxed reporting mode. For many detectors, this option suppresses the heuristics used to avoid reporting false
     * positives.
     *
     * @since 1.1
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.relaxed')
    boolean relaxed

    /**
     * The visitor list to run. This is a comma-delimited list.
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = 'spotbugs.visitors')
    String visitors

    /**
     * The visitor list to omit. This is a comma-delimited list.
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = 'spotbugs.omitVisitors')
    String omitVisitors

    /**
     * Selectively enable/disable detectors. This is a comma-delimited list with "+" or "-" before each detectors name indicated enabling or disabling.
     *
     * @since 4.9.4.2
     */
    @Parameter(property = 'spotbugs.chooseVisitors')
    String chooseVisitors

    /**
     * The plugin list to include in the report. This is a comma-delimited list.
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a plugin file.
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = 'spotbugs.pluginList')
    String pluginList

    /**
     * Collection of PluginArtifact to work on. (PluginArtifact contains groupId, artifactId, version, type, classifier.)
     * See <a href="./usage.html#Using Detectors from a Repository">Usage</a> for details.
     * <p>
     * As an alternative, SpotBugs extension plugin JARs can also be declared as standard Maven
     * {@code <dependencies>} of this plugin. Any dependency whose JAR contains
     * {@code findbugs.xml} is automatically detected and passed to SpotBugs.
     *
     * @since 2.4.1
     * @since 4.8.3.0 includes classifier
     */
    @Parameter
    List<PluginArtifact> plugins

    /**
     * Restrict analysis to the given comma-separated list of classes and packages.
     *
     * @since 1.1
     */
    @Parameter(property = 'spotbugs.onlyAnalyze')
    String onlyAnalyze

    /**
     * This option enables or disables scanning of nested jar and zip files found
     * in the list of files and directories to be analyzed.
     *
     * @since 2.3.2
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.nested')
    boolean nested

    /**
     * Prints a trace of detectors run and classes analyzed to standard output.
     * Useful for troubleshooting unexpected analysis failures.
     *
     * @since 2.3.2
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.trace')
    boolean trace

    /**
     * Maximum bug ranking to record.
     *
     * @since 2.4.1
     */
    @Parameter(property = 'spotbugs.maxRank')
    int maxRank

    /**
     * Skip entire check.
     *
     * @since 1.1
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.skip')
    boolean skip

    /**
     * Resource Manager.
     *
     * @since 2.0
     */
    @Inject
    ResourceManager resourceManager

    /**
     * The Maven Settings object, used to look up server credentials for authenticated URLs.
     */
    @Parameter(defaultValue = '${settings}', readonly = true)
    Settings settings

    /**
     * Settings decrypter used to decrypt passwords stored in Maven settings.
     */
    @Inject
    SettingsDecrypter settingsDecrypter

    /**
     * The id of a server configured in Maven's {@code settings.xml} whose credentials
     * (username and password) will be used when fetching filter or baseline files from
     * an {@code http://} or {@code https://} URL.
     * <p>
     * Example {@code settings.xml} entry:
     * <pre>{@code
     * <server>
     *   <id>my-nexus</id>
     *   <username>user</username>
     *   <password>secret</password>
     * </server>
     * }</pre>
     * Example usage:
     * <pre>{@code
     * <configuration>
     *   <excludeFilterFile>https://nexus.example.com/config/spotbugs-exclude.xml</excludeFilterFile>
     *   <filterServerId>my-nexus</filterServerId>
     * </configuration>
     * }</pre>
     *
     * @since 4.9.8.4
     */
    @Parameter(property = 'spotbugs.filterServerId')
    String filterServerId

    /**
     * Fail the build on an error.
     *
     * @since 2.0
     */
    @Parameter(defaultValue = 'true', property = 'spotbugs.failOnError')
    boolean failOnError

    /**
     * Fork a VM for Spotbugs analysis.  This will allow you to set timeouts and heap size.
     *
     * @since 2.3.2
     */
    @Parameter(defaultValue = 'true', property = 'spotbugs.fork')
    boolean fork

    /**
     * Maximum Java heap size in megabytes  (default=512).
     * This only works if the <b>fork</b> parameter is set <b>true</b>.
     *
     * @since 2.2
     */
    @Parameter(defaultValue = '512', property = 'spotbugs.maxHeap')
    int maxHeap

    /**
     * Specifies the amount of time, in milliseconds, that Spotbugs may run before
     * it is assumed to be hung and is terminated.
     * The default is 600,000 milliseconds, which is ten minutes.
     * This only works if the <b>fork</b> parameter is set <b>true</b>.
     *
     * @since 2.2
     */
    @Parameter(defaultValue = '600000', property = 'spotbugs.timeout')
    int timeout

    /**
     * The arguments to pass to the forked VM (ignored if fork is disabled).
     *
     * @since 2.4.1
     */
    @Parameter(property = 'spotbugs.jvmArgs')
    String jvmArgs

    /**
     * Skip the Spotbugs HTML report generation if there are no violations found. Defaults to
     * <code>false</code>.
     *
     * @since 3.0.1
     */
    @Parameter(defaultValue = "false", property = 'spotbugs.skipEmptyReport')
    boolean skipEmptyReport

    /**
     * Set the path of the user preferences file to use.
     * Will try to read the path as a resource before treating it as a local path.
     *
     * This will read in a configuration file to set up Spotbugs.
     *
     * The parameters in the POM file will override anything in the config file
     *
     * @since 3.0.2
     */
    @Parameter(property = 'spotbugs.userPrefs')
    String userPrefs

    /**
     * System properties to set in the VM (or the forked VM if fork is enabled).
     *
     * @since 4.3.0
     */
    @Parameter(property = 'spotbugs.systemPropertyVariables')
    Map<String, String> systemPropertyVariables

    /**
     * Map of SpotBugs plugin IDs to their documentation URL templates.
     * The placeholder {@code {type}} in the template is replaced with the bug type code when
     * generating report links.
     * <p>
     * Built-in defaults are already provided for the following well-known plugins:
     * <ul>
     *   <li>{@code com.mebigfatguy.fbcontrib} &rarr;
     *       {@code https://fb-contrib.sourceforge.net/bugdescriptions.html#{type}}</li>
     *   <li>{@code com.h3xstream.findsecbugs} &rarr;
     *       {@code https://find-sec-bugs.github.io/bugs.htm#{type}}</li>
     * </ul>
     * Entries provided here override the built-in defaults, allowing you to add support for
     * other SpotBugs addon plugins or customise the URLs for the ones above.
     * The plugin ID can be found in the {@code pluginid} attribute of the plugin's
     * {@code findbugs.xml} descriptor.
     * <p>
     * Example:
     * <pre>{@code
     * <pluginDocumentationUrls>
     *   <com.example.myplugin>https://example.com/bugs.html#{type}</com.example.myplugin>
     * </pluginDocumentationUrls>
     * }</pre>
     *
     * @since 4.9.4.3
     */
    @Parameter
    Map<String, String> pluginDocumentationUrls

    /** The bug count. */
    int bugCount

    /** The error count. */
    int errorCount

    /** The resource bundle. */
    ResourceBundle bundle

    /** The output spotbugs file. */
    File outputSpotbugsFile

    /**
     * Checks whether prerequisites for generating this report are given.
     *
     * @return true if report can be generated, otherwise false
     * @see AbstractMavenReport#canGenerateReport()
     */
    @Override
    boolean canGenerateReport() {
        if (skip) {
            log.info('Spotbugs plugin skipped')
            return false
        }

        executeCheck()

        boolean canGenerate = false
        log.debug('****** SpotBugsMojo canGenerateReport *******')

        Predicate<Path> containsSource = { Path path ->
            String fileName = path.toFile().name
            return fileName.endsWith(SpotBugsInfo.CLASS_SUFFIX) ||
                (nested && (fileName.endsWith(SpotBugsInfo.JAR_SUFFIX) || fileName.endsWith(SpotBugsInfo.ZIP_SUFFIX)))
        }

        if (classFilesDirectory.exists()) {
            if (noClassOk) {
                canGenerate = true
            } else {
                try {
                    canGenerate = Files.walk(classFilesDirectory.toPath())
                        .anyMatch(containsSource)
                } catch (IOException e) {
                    log.warn("Error searching class files: ${e.message}")
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("canGenerate Src is ${canGenerate}")
            }
        }

        if (!canGenerate && testClassFilesDirectory.exists() && includeTests) {
            if (noClassOk) {
                canGenerate = true
            } else {
                try {
                    canGenerate = Files.walk(testClassFilesDirectory.toPath())
                        .anyMatch(containsSource)
                } catch (IOException e) {
                    log.warn("Error searching test class files: ${e.message}")
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("canGenerate Test Src is ${canGenerate}")
            }
        }

        if (canGenerate && outputSpotbugsFile == null) {
            outputSpotbugsFile = spotbugsXmlOutputDirectory.toPath().resolve(spotbugsXmlOutputFilename).toFile()
            executeSpotbugs(outputSpotbugsFile)
            if (skipEmptyReport && bugCount == 0) {
                canGenerate = false
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("canGenerate is ${canGenerate}")
        }

        boolean isSiteLifecycle = false
        if (session != null && session.getRequest() != null) {
            List<String> goals = session.getRequest().getGoals()
            if (goals != null && goals.any { String goal ->
                goal.contains('site')
            }) {
                isSiteLifecycle = true
            }
        }

        if (canGenerate) {
            if (!isSiteLifecycle) {
                // Only generate xdoc report, skip site pages
                generateXDoc(getLocale())
                return false
            }
        } else {
            log.info('No files found to run spotbugs; check compile phase has been run.')
        }

        return canGenerate
    }

    /**
     * Returns the plugins description for the "generated reports" overview page.
     *
     * @param locale
     *            the locale the report should be generated for
     *
     * @return description of the report
     * @see MavenReport#getDescription(Locale)
     */
    @Override
    String getDescription(Locale locale) {
        return getBundle(locale).getString(SpotBugsInfo.DESCRIPTION_KEY)
    }

    /**
     * Returns the plugins name for the "generated reports" overview page and the menu.
     *
     * @param locale
     *            the locale the report should be generated for
     *
     * @return name of the report
     * @see MavenReport#getName(Locale)
     */
    @Override
    String getName(Locale locale) {
        return getBundle(locale).getString(SpotBugsInfo.NAME_KEY)
    }

    /**
     * Returns report output file name, without the extension.
     *
     * Called by AbstractMavenReport.execute() for creating the sink.
     *
     * @return name of the generated page
     * @see {@link MavenReport#getOutputName()}
     *
     * @deprecated Method name does not properly reflect its purpose. Implement and use
     * {@link #getOutputPath()} instead. This is waiting on maven to switch in report
     * plugin before we can remove it.
     */
    @Override
    @Deprecated
    String getOutputName() {
        return SpotBugsInfo.PLUGIN_NAME
    }

    /**
     * Returns report output file name, without the extension.
     *
     * Called by AbstractMavenReport.execute() for creating the sink.
     *
     * @return name of the generated page
     * @see {@link MavenReport#getOutputPath()}
     */
    @Override
    String getOutputPath() {
        return SpotBugsInfo.PLUGIN_NAME
    }

    /**
     * Executes the generation of the report.
     *
     * Callback from Maven Site Plugin.
     *
     * @param locale the wanted locale to generate the report, could be null.
     *
     * @see AbstractMavenReport#executeReport(Locale)
     */
    @Override
    void executeReport(Locale locale) {
        log.debug('****** SpotBugsMojo executeReport *******')
        if (!canGenerateReport()) {
            return
        }

        if (log.isDebugEnabled()) {
            log.debug("Locale is ${locale.getLanguage()}")
            log.debug('****** SpotBugsMojo executeReport *******')
            log.debug('report Output Directory is ' + getReportOutputDirectory())
            log.debug('Output Directory is ' + outputDirectory)
            log.debug('Classes Directory is ' + classFilesDirectory)
            log.debug("  Plugin Artifacts to be added -> ${pluginArtifacts}")
        }

        generateXDoc(locale)

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new MojoExecutionException('Cannot create html output directory')
        }

        if (outputSpotbugsFile != null && outputSpotbugsFile.exists()) {

            if (skipEmptyReport && bugCount == 0) {
                log.info('Skipping Generation Spotbugs HTML since there are not any bugs')
            } else {
                log.debug('Generating Spotbugs HTML')

                SpotbugsReportGenerator generator = new SpotbugsReportGenerator(getSink(), getBundle(locale))

                boolean isJxrPluginEnabled = isJxrPluginEnabled()

                generator.setIsJXRReportEnabled(isJxrPluginEnabled)

                if (isJxrPluginEnabled) {
                    generator.setCompileSourceRoots(session.getCurrentProject().compileSourceRoots)
                    generator.setTestSourceRoots(session.getCurrentProject().testCompileSourceRoots)
                    generator.setXrefLocation(this.xrefLocation)
                    generator.setXrefTestLocation(this.xrefTestLocation)
                    generator.setIncludeTests(this.includeTests)
                }

                generator.setLog(log)
                generator.threshold = threshold
                generator.effort = effort
                generator.bugTypeUrlMap = buildBugTypeUrlMap(pluginDocumentationUrls)

                XmlSlurper xmlSlurper = new XmlSlurper()
                xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
                xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

                generator.setSpotbugsResults(xmlSlurper.parse(outputSpotbugsFile))
                generator.setOutputDirectory(new File(outputDirectory.getAbsolutePath()))
                generator.generateReport()

                if (log.isDebugEnabled()) {
                    log.debug("xmlOutput is ${xmlOutput}")
                }
            }
        }
    }

    private void executeCheck() {
        log.debug('****** SpotBugsMojo executeCheck *******')

        log.debug('Generating Spotbugs XML')

        if (!spotbugsXmlOutputDirectory.exists() && !spotbugsXmlOutputDirectory.mkdirs()) {
            throw new MojoExecutionException('Cannot create xml output directory.')
        }
    }

    private void generateXDoc(Locale locale) {
        log.debug('****** SpotBugsMojo generateXDoc *******')

        if (outputSpotbugsFile == null || !outputSpotbugsFile.exists()) {
            return
        }

        if (log.isDebugEnabled()) {
            log.debug("xmlOutput is ${xmlOutput}")
        }

        if (xmlOutput) {
            log.debug('  Using the xdoc format')

            if (!xmlOutputDirectory.exists() && !xmlOutputDirectory.mkdirs()) {
                throw new MojoExecutionException('Cannot create xdoc output directory')
            }

            XDocsReporter xDocsReporter = new XDocsReporter(getBundle(locale), log, threshold, effort, outputEncoding)
            xDocsReporter.setOutputWriter(Files.newBufferedWriter(Path.of("${xmlOutputDirectory}/spotbugs.xml"),
                Charset.forName(outputEncoding)))

            XmlSlurper xmlSlurper = new XmlSlurper()
            xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
            xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

            xDocsReporter.setSpotbugsResults(xmlSlurper.parse(outputSpotbugsFile))
            xDocsReporter.setCompileSourceRoots(session.getCurrentProject().compileSourceRoots)
            xDocsReporter.setTestSourceRoots(session.getCurrentProject().testCompileSourceRoots)

            xDocsReporter.generateReport()
        }
    }

    /**
     * Returns the report output directory allowed to be not read only as defined in AbstractMavenParent.
     *
     * Called by AbstractMavenReport.execute() for creating the sink.
     *
     * @return full path to the directory where the files in the site get copied to
     * @see AbstractMavenReport#getOutputDirectory()
     */
    @Override
    protected String getOutputDirectory() {
        return outputDirectory.getAbsolutePath()
    }

    /**
     * Determines if the JXR-Plugin is included in the report section of the POM.
     *
     * @param bundle
     *            The bundle to load the artifactIf of the jxr plugin.
     * @return True if the JXR-Plugin is included in the POM, false otherwise.
     *
     */
    protected boolean isJxrPluginEnabled() {
        if (xrefLocation.exists()) {
            return true
        }

        List<ReportPlugin> reportPlugins = session.getCurrentProject().getModel().getReporting().getPlugins()

        boolean isEnabled

        reportPlugins.each() { ReportPlugin reportPlugin ->

            if (log.isDebugEnabled()) {
                log.debug("report plugin -> ${reportPlugin.getArtifactId()}")
            }

            if ('maven-jxr-plugin'.equals(reportPlugin.getArtifactId())) {
                isEnabled = true
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("jxr report links are ${isEnabled ? 'enabled' : 'disabled'}")
        }
        return isEnabled
    }

    ResourceBundle getBundle(Locale locale) {

        this.bundle = ResourceBundle.getBundle(SpotBugsInfo.BUNDLE_NAME, locale, SpotBugsMojo.class.getClassLoader())

        if (log.isDebugEnabled()) {
            log.debug('Mojo Locale is ' + this.bundle.getLocale().getLanguage())
        }

        return bundle
    }

    /**
     * Get the Spotbugs command line arguments.
     *
     * @param htmlTempFile Spotbugs html temp output file
     * @param xmlTempFile Spotbugs xml temp output file
     * @param sarifTempFile Spotbugs sarif temp output file
     * @param auxClasspathFile Spotbugs auxclasspath file, or null if not needed
     *
     * @return Spotbugs command line arguments.
     *
     */
    private ArrayList<String> getSpotbugsArgs(File htmlTempFile, File xmlTempFile, File sarifTempFile,
            File auxClasspathFile) {
        String httpUser = null
        String httpPassword = null
        if (filterServerId) {
            def server = settings?.getServer(filterServerId)
            if (server != null) {
                if (settingsDecrypter == null) {
                    log.debug("settingsDecrypter is unavailable; using raw server credentials for filterServerId '${filterServerId}'")
                }
                def decrypted = settingsDecrypter?.decrypt(new DefaultSettingsDecryptionRequest(server))
                def decryptedServer = decrypted?.getServer() ?: server
                httpUser = decryptedServer.getUsername()
                httpPassword = decryptedServer.getPassword()
            } else {
                log.warn("filterServerId '${filterServerId}' not found in Maven settings")
            }
        }
        ResourceHelper resourceHelper =
            new ResourceHelper(log, spotbugsXmlOutputDirectory, resourceManager, repositorySystem, session,
                httpUser, httpPassword)
        List<String> args = []

        if (userPrefs) {
            if (log.isDebugEnabled()) {
                log.debug("  Adding User Preferences File -> ${userPrefs}")
            }

            args << '-userPrefs'
            args << resourceHelper.getResourceFile(userPrefs.trim()).getAbsolutePath()
        }

        if (htmlOutput) {
            log.debug("  Adding 'htmlOutput'")
            args << '-html=' + htmlTempFile.getAbsolutePath()
        }

        log.debug("  Adding 'xml:withMessages'")
        args << '-xml:withMessages=' + xmlTempFile.getAbsolutePath()

        if (sarifOutput) {
            log.debug("  Adding 'sarifOutput'")
            args << '-sarif=' + sarifTempFile.getAbsolutePath()
        }

        if (auxClasspathFile) {
            log.debug("  Adding 'auxclasspathFromFile'")
            args << '-auxclasspathFromFile'
            args << auxClasspathFile.getAbsolutePath()
        }

        log.debug("  Adding 'projectName'")
        args << '-projectName'
        args << project.name

        log.debug("  Adding 'effortParameter'")
        args << getEffortParameter()

        log.debug("  Adding 'thresholdParameter'")
        args << getThresholdParameter()

        if (debug) {
            log.debug("  Adding 'progress'")
            args << '-progress'
        }

        String spotbugsPlugins = getSpotbugsPlugins()
        if (spotbugsPlugins) {
            log.debug("  Adding 'pluginList'")
            args << '-pluginList'
            args << spotbugsPlugins
        }


        if (visitors) {
            log.debug("  Adding 'visitors'")
            args << '-visitors'
            args << visitors
        }

        if (omitVisitors) {
            log.debug("  Adding 'omitVisitors'")
            args << '-omitVisitors'
            args << omitVisitors
        }

        if (chooseVisitors) {
            log.debug("  Adding 'chooseVisitors'")
            args << '-chooseVisitors'
            args << chooseVisitors
        }

        if (relaxed) {
            log.debug("  Adding 'relaxed'")
            args << '-relaxed'
        }

        if (nested) {
            log.debug("  Adding 'nested:true'")
            args << '-nested:true'
        } else {
            log.debug("  Adding 'nested:false'")
            args << '-nested:false'
        }

        if (onlyAnalyze) {
            log.debug("  Adding 'onlyAnalyze'")
            args << '-onlyAnalyze'
            args << Arrays.stream(onlyAnalyze.split(SpotBugsInfo.COMMA)).map { String string ->
                string.startsWith('file:') ? Files.lines(resourceHelper.getResourceFile(string.substring(5)).toPath())
                    .collect(Collectors.joining(SpotBugsInfo.COMMA)) : string
            }.collect(Collectors.joining(','))
        }

        if (includeFilterFile) {
            log.debug('  Adding Include Filter File')

            List<String> includefilters = Arrays.asList(includeFilterFile.split(SpotBugsInfo.COMMA))

            includefilters.each { String includefilter ->
                args << '-include'
                args << resourceHelper.getResourceFile(includefilter.trim()).getAbsolutePath()
            }
        }

        if (includeFilterFiles) {
            log.debug('  Adding Include Filter Files')

            includeFilterFiles.each { String includefilter ->
                args << '-include'
                args << resourceHelper.getResourceFile(includefilter.trim()).getAbsolutePath()
            }
        }

        if (excludeFilterFile) {
            log.debug('  Adding Exclude Filter File')
            List<String> excludefilters = Arrays.asList(excludeFilterFile.split(SpotBugsInfo.COMMA))

            excludefilters.each { String excludeFilter ->
                args << '-exclude'
                args << resourceHelper.getResourceFile(excludeFilter.trim()).getAbsolutePath()
            }
        }

        if (excludeFilterFiles) {
            log.debug('  Adding Exclude Filter Files')

            excludeFilterFiles.each { String excludeFilter ->
                args << '-exclude'
                args << resourceHelper.getResourceFile(excludeFilter.trim()).getAbsolutePath()
            }
        }

        if (excludeBugsFile) {
            log.debug('  Adding Exclude Bug File (Baselines)')
            List<String> excludeFiles = Arrays.asList(excludeBugsFile.split(SpotBugsInfo.COMMA))

            excludeFiles.each() { String excludeFile ->
                args << '-excludeBugs'
                args << resourceHelper.getResourceFile(excludeFile.trim()).getAbsolutePath()
            }
        }

        if (excludeBugsFiles) {
            log.debug('  Adding Exclude Bug Files (Baselines)')

            excludeBugsFiles.each() { String excludeFile ->
                args << '-excludeBugs'
                args << resourceHelper.getResourceFile(excludeFile.trim()).getAbsolutePath()
            }
        }

        if (addSourceDirs) {
            log.debug('  Adding Source directories (To process source exclusions)')
            args << '-sourcepath'
            String sourceRoots = ''
            session.getCurrentProject().compileSourceRoots.each { String sourceRoot ->
                sourceRoots += sourceRoot + File.pathSeparator
            }
            if (includeTests) {
                session.getCurrentProject().testCompileSourceRoots.each { String testSourceRoot ->
                    sourceRoots += testSourceRoot + File.pathSeparator
                }
            }
            args << sourceRoots.substring(0, sourceRoots.length() -1)
        }

        if (maxRank) {
            log.debug("  Adding 'maxRank'")
            args << '-maxRank'
            args << String.valueOf(maxRank)
        }

        if (noClassOk) {
            log.debug("  Adding 'noClassOk'")
            args << '-noClassOk'
        }

        if (classFilesDirectory.isDirectory()) {
            if (log.isDebugEnabled()) {
                log.debug('  Adding to Source Directory -> ' + classFilesDirectory.absolutePath)
            }
            args << classFilesDirectory.absolutePath
        }

        if (testClassFilesDirectory.isDirectory() && includeTests) {
            if (log.isDebugEnabled()) {
                log.debug('  Adding to Source Directory -> ' + testClassFilesDirectory.absolutePath)
            }
            args << testClassFilesDirectory.absolutePath
        }

        return args
    }

    /**
     * Create the Spotbugs AuxClasspath file in the project build directory.
     * The caller is responsible for deleting the returned file when it is no longer needed.
     *
     * @return the auxclasspath file, or null if no auxclasspath elements are available
     */
    private File createSpotbugsAuxClasspathFile() {
        List<String> auxClasspathElements

        if (testClassFilesDirectory.isDirectory() && includeTests) {
            auxClasspathElements = session.getCurrentProject().testClasspathElements
        } else if (classFilesDirectory.isDirectory()) {
            auxClasspathElements = session.getCurrentProject().compileClasspathElements
        }

        File auxClasspathFile = null

        if (auxClasspathElements) {
            if (log.isDebugEnabled()) {
                log.debug('  AuxClasspath Elements -> ' + auxClasspathElements)
            }

            List<String> auxClasspathList = auxClasspathElements.findAll { String auxClasspath ->
                if (session.getCurrentProject().getBuild().outputDirectory == auxClasspath) {
                    return false
                }
                // Exclude JAR files that contain classes in the java.* package. Such JARs (e.g.
                // JavaCard API jars) provide partial re-implementations of bootstrap JDK classes.
                // Including them in the auxClasspath causes SpotBugs to use the incomplete java.*
                // versions instead of the real JDK classes, resulting in "missing classes" warnings
                // for standard JDK types. The JDK always provides the authoritative java.* classes
                // via the jrt:/ filesystem, so these JARs must never appear in the auxClasspath.
                if (containsJdkClasses(auxClasspath)) {
                    if (log.isDebugEnabled()) {
                        log.debug("  Excluding from auxClasspath (contains java.* class overrides): ${auxClasspath}")
                    }
                    return false
                }
                return true
            }
            if (auxClasspathList.size() > 0) {
                if (log.isDebugEnabled()) {
                    log.debug('  Last AuxClasspath is -> ' + auxClasspathList[auxClasspathList.size() - 1])
                }

                auxClasspathFile = new File(project.build.directory, 'spotbugsAuxClasspath.tmp')
                Files.createDirectories(auxClasspathFile.toPath().getParent())

                if (log.isDebugEnabled()) {
                    auxClasspathList.each { String auxClasspathElement ->
                        log.debug('  Adding to AuxClasspath -> ' + auxClasspathElement)
                    }
                }
                auxClasspathFile.text = auxClasspathList.join(SpotBugsInfo.EOL) + SpotBugsInfo.EOL
            }
        }

        return auxClasspathFile
    }

    /**
     * Checks whether the given path is a JAR file that contains class files in the
     * {@code java.*} package. Such JARs shadow JDK bootstrap classes and must be
     * excluded from the SpotBugs auxClasspath.
     *
     * @param path the filesystem path to check
     * @return {@code true} if the path is a JAR containing {@code java.*} classes
     */
    private boolean containsJdkClasses(String path) {
        if (!path.endsWith('.jar')) {
            return false
        }
        File jarFile = new File(path)
        if (!jarFile.isFile()) {
            return false
        }
        try {
            new JarFile(jarFile).withCloseable { jar ->
                return jar.entries().asIterator().any { entry ->
                    !entry.isDirectory() && entry.name.startsWith('java/') && entry.name.endsWith('.class')
                }
            }
        } catch (IOException e) {
            log.debug("Cannot inspect jar file for JDK class overrides: ${path}")
            return false
        }
    }

    /**
     * For the file creation by creating the file AND folder if needed.
     * The file created will be empty.
     *
     * @param file Destination file to create.
     */
    private static void forceFileCreation(File file) {
        if (file.exists()) {
            file.delete()
        }

        Files.createDirectories(file.toPath().getParent())
        file.createNewFile()
    }

    /**
     * Set up and run the Spotbugs engine.
     *
     * @param outputFile
     *            the outputFile
     *
     */
    private void executeSpotbugs(File outputFile) {

        log.debug('****** SpotBugsMojo executeSpotbugs *******')

        File htmlTempFile = new File("${outputDirectory}/spotbugs.html")
        if (htmlOutput) {
            forceFileCreation(htmlTempFile)
        }

        File xmlTempFile = new File("${project.build.directory}/spotbugsTemp.xml")
        forceFileCreation(xmlTempFile)

        File sarifTempFile = new File("${project.build.directory}/spotbugsTempSarif.json")
        if (sarifOutput) {
            forceFileCreation(sarifTempFile)
        }

        outputEncoding = outputEncoding ?: StandardCharsets.UTF_8

        log.debug('****** Executing SpotBugsMojo *******')

        resourceManager.addSearchPath(FileResourceLoader.ID, session.getCurrentProject().getFile()
            .getParentFile().getAbsolutePath())
        resourceManager.addSearchPath(SpotBugsInfo.URL, '')

        resourceManager.setOutputDirectory(new File(session.getCurrentProject().getBuild().directory))

        if (log.isDebugEnabled()) {
            log.debug("resourceManager.outputDirectory is ${resourceManager.outputDirectory}")
            log.debug("Plugin Artifacts to be added -> ${pluginArtifacts}")
            log.debug("outputFile is ${outputFile.getCanonicalPath()}")
            log.debug("output Directory is ${spotbugsXmlOutputDirectory.getAbsolutePath()}")
            if (htmlOutput) {
                log.debug("HtmlTempFile is ${htmlTempFile.getCanonicalPath()}")
            }
            log.debug("XmlTempFile is ${xmlTempFile.getCanonicalPath()}")
            if (sarifOutput) {
                log.debug("SarifTempFile is ${sarifTempFile.getCanonicalPath()}")
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Fork Value is ${fork}")
        }

        long startTime
        if (debug) {
            startTime = System.nanoTime()
        }

        File auxClasspathFile = createSpotbugsAuxClasspathFile()

        try {
            List<String> spotbugsArgs = getSpotbugsArgs(htmlTempFile, xmlTempFile, sarifTempFile, auxClasspathFile)

        Charset effectiveEncoding
        if (sourceEncoding) {
            effectiveEncoding = Charset.forName(sourceEncoding)
        } else {
            effectiveEncoding = Charset.defaultCharset() ?: StandardCharsets.UTF_8
        }
        if (log.isDebugEnabled()) {
            log.debug('File Encoding is ' + effectiveEncoding.name())
        }

        AntBuilder ant = new AntBuilder()
        Map<String, Object> javaTaskParams = [classname: 'edu.umd.cs.findbugs.FindBugs2', fork: "${fork}",
                failonerror: 'true', clonevm: 'false', timeout: timeout, maxmemory: "${maxHeap}m"]
        def toolchain = toolchainManager?.getToolchainFromBuildContext('jdk', session)
        String javaExecutable = toolchain?.findTool('java')
        if (javaExecutable) {
            if (fork) {
                log.info("Toolchain in spotbugs-maven-plugin: ${toolchain}")
                javaTaskParams['executable'] = javaExecutable
            } else {
                log.warn('Toolchain is configured but fork is disabled. The toolchain JVM will not be used.')
            }
        }
        ant.java(javaTaskParams) {

            sysproperty(key: 'file.encoding', value: effectiveEncoding.name())

            if (jvmArgs && fork) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding JVM Args => ${jvmArgs}")
                }

                List<String> args = Arrays.asList(jvmArgs.split(SpotBugsInfo.BLANK))

                args.each() { String jvmArg ->
                    if (log.isDebugEnabled()) {
                        log.debug("Adding JVM Arg => ${jvmArg}")
                    }
                    jvmarg(value: jvmArg)
                }
            }

            if (debug || trace) {
                sysproperty(key: 'findbugs.debug', value: Boolean.TRUE)
            }

            classpath() {

                pluginArtifacts.each() { Artifact pluginArtifact ->
                    if (log.isDebugEnabled()) {
                        log.debug("  Adding to pluginArtifact -> ${pluginArtifact.file}")
                    }

                    pathelement(location: pluginArtifact.file)
                }
            }

            spotbugsArgs.each { String spotbugsArg ->
                if (log.isDebugEnabled()) {
                    log.debug("Spotbugs arg is ${spotbugsArg}")
                }
                arg(value: spotbugsArg)
            }

            systemPropertyVariables.each { Map.Entry<String, String> sysProp ->
                if (log.isDebugEnabled()) {
                    log.debug("System property ${sysProp.key} is ${sysProp.value}")
                }
                sysproperty(key: sysProp.key, value: sysProp.value)
            }
        }

        long duration
        if (debug) {
            duration = (System.nanoTime() - startTime) / 1000000000.00
            log.debug("SpotBugs duration is ${duration}")
        }

        log.info('Done SpotBugs Analysis....')

        if (htmlTempFile.exists() && htmlOutput && htmlTempFile.size() > 0) {
            // Do nothing more at this time
            log.debug('Html temp file exixts with content....')
        }

        if (xmlTempFile.exists()) {
            if (xmlTempFile.size() > 0) {
                XmlSlurper xmlSlurper = new XmlSlurper()
                xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
                xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

                GPathResult path = xmlSlurper.parse(xmlTempFile)

                List<NodeChild> allNodes = path.depthFirst().toList()

                bugCount = allNodes.count { NodeChild node -> node.name() == 'BugInstance' }
                if (log.isDebugEnabled()) {
                    log.debug("BugInstance size is ${bugCount}")
                }

                errorCount = allNodes.count { NodeChild node -> node.name() == 'Error' }
                if (log.isDebugEnabled()) {
                    log.debug("Error size is ${errorCount}")
                }

                NodeChildren xmlProject = path.Project

                session.getCurrentProject().compileSourceRoots.each() { String compileSourceRoot ->
                    xmlProject.appendNode { SrcDir(compileSourceRoot) }
                }

                if (testClassFilesDirectory.isDirectory() && includeTests) {
                    session.getCurrentProject().testCompileSourceRoots.each() { String testSourceRoot ->
                        xmlProject.appendNode { SrcDir(testSourceRoot) }
                    }
                }

                // Fixes visitor problem
                path.SpotbugsResults.FindBugsSummary.'total_bugs' = bugCount

                xmlProject.appendNode {
                    WrkDir(session.getCurrentProject().getBuild().directory)
                }

                StreamingMarkupBuilder xmlBuilder = new StreamingMarkupBuilder()

                if (outputFile.exists()) {
                    outputFile.delete()
                }

                Files.createDirectories(outputFile.toPath().getParent())
                outputFile.createNewFile()

                BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), effectiveEncoding)

                if (effectiveEncoding.name().equalsIgnoreCase('Cp1252')) {
                    writer.write '<?xml version="1.0" encoding="windows-1252"?>'
                } else {
                    writer.write '<?xml version="1.0" encoding="' +
                        effectiveEncoding.name().toLowerCase(Locale.getDefault()) + '"?>'
                }

                writer.write SpotBugsInfo.EOL

                writer << xmlBuilder.bind { mkp.yield path }
            } else {
                log.info('No bugs found')
                if (noClassOk) {
                    log.info('No class files to analyze; creating empty output due to noClassOk=true')
                    Charset effectiveEncodingForEmpty = outputEncoding ?: StandardCharsets.UTF_8
                    Files.createDirectories(outputFile.toPath().getParent())
                    String minimalXml = '<?xml version="1.0" encoding="' +
                        effectiveEncodingForEmpty.name().toLowerCase(Locale.ROOT) + '"?>' +
                        SpotBugsInfo.EOL + '<BugCollection></BugCollection>'
                    Files.write(outputFile.toPath(), minimalXml.getBytes(effectiveEncodingForEmpty))
                }
            }

            // Do not delete file when running under debug mode
            if (!debug) {
                xmlTempFile.delete()
            }
        }

        if (sarifTempFile && sarifOutput && sarifTempFile.size() > 0) {

            Map<String, Object> slurpedResult = new JsonSlurper().parse(sarifTempFile)
            JsonBuilder builder = new JsonBuilder(slurpedResult)

            // With -Dspotbugs.sarifFullPath=true
            // The location uri will be replace by path relative to the root of project
            // SomeFile.java => src/main/java/SomeFile.java
            // This change is required for some tool including Github code scanning API
            if (sarifFullPath) {

                SourceFileIndexer indexer = new SourceFileIndexer()

                indexer.buildListSourceFiles(session)

                for (result in slurpedResult.runs.results[0]) {

                    for (loc in result.locations) {
                        String originalFullPath = loc.physicalLocation.artifactLocation.uri

                        //We replace relative path to the complete path
                        String newFileName = indexer.searchActualFilesLocation(originalFullPath)

                        if (newFileName != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("${originalFullPath} modified to ${newFileName}")
                            }
                            loc.physicalLocation.artifactLocation.uri = newFileName
                        } else if (log.isWarnEnabled()) {
                            log.warn("No source file found for ${originalFullPath}. "
                                + 'The path include in the SARIF report could be incomplete.')
                        }
                    }
                }
            }

            File sarifFinalFile = new File(sarifOutputDirectory, sarifOutputFilename)
            forceFileCreation(sarifFinalFile)

            sarifFinalFile.withWriter { BufferedWriter writer ->
                builder.writeTo(writer)
            }

            // Do not delete file when running under debug mode
            if (!debug) {
                sarifTempFile.delete()
            }
        }

        } finally {
            // Delete the auxclasspath temp file regardless of success or failure
            if (auxClasspathFile && !debug) {
                auxClasspathFile.delete()
            }
        }
    }

    /**
     * Returns the threshold parameter to use.
     *
     * @return A valid threshold parameter.
     *
     */
    protected String getThresholdParameter() {

        if (log.isDebugEnabled()) {
            log.debug("threshold is ${threshold}")
        }

        String thresholdParameter
        switch (threshold) {
            case 'High':
                thresholdParameter = '-high'
                break

            case 'Exp':
                thresholdParameter = '-experimental'
                break

            case 'Low':
                thresholdParameter = '-low'
                break

            case 'high':
                thresholdParameter = '-high'
                break

            default:
                thresholdParameter = '-medium'
                break
        }

        if (log.isDebugEnabled()) {
            log.debug("thresholdParameter is ${thresholdParameter}")
        }

        return thresholdParameter
    }

    /**
     *  Set report output directory, allowed to be not read only as defined in AbstractMavenParent.
     *
     * @see AbstractMavenReport#setReportOutputDirectory(File)
     */
    @Override
    void setReportOutputDirectory(File reportOutputDirectory) {
        super.setReportOutputDirectory(reportOutputDirectory)
        this.outputDirectory = reportOutputDirectory
    }

    /**
     * Gets the Java executable to use for the forked SpotBugs process.
     * If a JDK toolchain is configured for the build, the executable from that toolchain is returned.
     * Otherwise, returns {@code null} and the default JVM will be used.
     *
     * @return the java executable path from the toolchain, or {@code null} if no toolchain is configured
     * @since 4.9.8.4
     */
    String getJavaExecutable() {
        def toolchain = toolchainManager?.getToolchainFromBuildContext('jdk', session)
        if (toolchain) {
            return toolchain.findTool('java')
        }
        return null
    }
}
