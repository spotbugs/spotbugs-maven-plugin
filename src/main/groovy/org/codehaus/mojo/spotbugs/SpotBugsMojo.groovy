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

import groovy.ant.AntBuilder
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChildren
import groovy.xml.StreamingMarkupBuilder

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

import javax.inject.Inject

import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.reporting.AbstractMavenReport
import org.apache.maven.reporting.MavenReport
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
    List pluginArtifacts

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

    /**
     * File name of the include filter. Only bugs in matching the filters are reported.
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
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
     * Potential values are a filesystem path, a URL, or a classpath resource.
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
    List includeFilterFiles

    /**
     * File name of the exclude filter. Bugs matching the filters are not reported.
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
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
    List excludeFilterFiles

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
    List excludeBugsFiles

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
     *
     * @since 2.4.1
     * @since 4.8.3.0 includes classifier
     */
    @Parameter
    PluginArtifact[] plugins

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

        boolean canGenerate
        log.debug('****** SpotBugsMojo canGenerateReport *******')

        if (!skip && classFilesDirectory.exists()) {

            classFilesDirectory.eachFileRecurse {
                if (it.name.contains(SpotBugsInfo.CLASS_SUFFIX)) {
                    canGenerate = true
                }
            }
            log.debug("canGenerate Src is ${canGenerate}")
        }

        if (!skip && testClassFilesDirectory.exists() && includeTests) {

            testClassFilesDirectory.eachFileRecurse {
                if (it.name.contains(SpotBugsInfo.CLASS_SUFFIX)) {
                    canGenerate = true
                }
            }
            log.debug("canGenerate Test Src is ${canGenerate}")
        }

        if (canGenerate && outputSpotbugsFile == null) {
            outputSpotbugsFile = new File("${spotbugsXmlOutputDirectory}/${spotbugsXmlOutputFilename}")

            executeSpotbugs(outputSpotbugsFile)

            if (skipEmptyReport && bugCount == 0) {
                canGenerate = false
            }
        }

        log.debug("canGenerate is ${canGenerate}")

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
     * @see (@link MavenReport#getOutputName()}
     *
     * @deprecated Method name does not properly reflect its purpose. Implement and use
     * {@link #getOutputPath()} instead.
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
        executeCheck()

        if (skip || !canGenerateReport()) {
            log.info('cannot generate report')
            return
        }

        if (log.isDebugEnabled()) {
            log.debug("Locale is ${locale.getLanguage()}")
            log.debug('****** SpotBugsMojo executeReport *******')
            log.debug('report Output Directory is ' + getReportOutputDirectory())
            log.debug('Output Directory is ' + outputDirectory)
            log.debug('Classes Directory is ' + classFilesDirectory)
            log.debug('  Plugin Artifacts to be added -> ' + pluginArtifacts.toString())
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
                generator.setSpotbugsResults(new XmlSlurper().parse(outputSpotbugsFile))
                generator.setOutputDirectory(new File(outputDirectory.getAbsolutePath()))
                generator.generateReport()
                log.debug("xmlOutput is ${xmlOutput}")
            }
        }
    }

    private void executeCheck() {
        log.debug('****** SpotBugsMojo executeCheck *******')

        log.debug('Generating Spotbugs XML')

        if (!spotbugsXmlOutputDirectory.exists() && !spotbugsXmlOutputDirectory.mkdirs()) {
            throw new MojoExecutionException('Cannot create xml output directory')
        }
    }

    private void generateXDoc(Locale locale) {
        log.debug('****** SpotBugsMojo generateXDoc *******')

        if (outputSpotbugsFile == null || !outputSpotbugsFile.exists()) {
            return
        }

        log.debug("xmlOutput is ${xmlOutput}")

        if (xmlOutput) {
            log.debug('  Using the xdoc format')

            if (!xmlOutputDirectory.exists() && !xmlOutputDirectory.mkdirs()) {
                throw new MojoExecutionException('Cannot create xdoc output directory')
            }

            XDocsReporter xDocsReporter = new XDocsReporter(getBundle(locale), log, threshold, effort, outputEncoding)
            xDocsReporter.setOutputWriter(Files.newBufferedWriter(Path.of("${xmlOutputDirectory}/spotbugs.xml"),
                Charset.forName(outputEncoding)))
            xDocsReporter.setSpotbugsResults(new XmlSlurper().parse(outputSpotbugsFile))
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

        List reportPlugins = session.getCurrentProject().getModel().getReporting().getPlugins()

        boolean isEnabled

        reportPlugins.each() { reportPlugin ->

            log.debug("report plugin -> ${reportPlugin.getArtifactId()}")
            if ('maven-jxr-plugin'.equals(reportPlugin.getArtifactId())) {
                isEnabled = true
            }
        }

        log.debug("jxr report links are ${isEnabled ? 'enabled' : 'disabled'}")
        return isEnabled
    }

    ResourceBundle getBundle(locale) {

        this.bundle = ResourceBundle.getBundle(SpotBugsInfo.BUNDLE_NAME, locale, SpotBugsMojo.class.getClassLoader())

        log.debug('Mojo Locale is ' + this.bundle.getLocale().getLanguage())

        return bundle
    }

    /**
     * Get the Spotbugs command line arguments.
     *
     * @param htmlTempFile Spotbugs html temp output file
     * @param xmlTempFile Spotbugs xml temp output file
     * @param sarifTempFile Spotbugs sarif temp output file
     *
     * @return Spotbugs command line arguments.
     *
     */
    private ArrayList<String> getSpotbugsArgs(File htmlTempFile, File xmlTempFile, File sarifTempFile) {
        ResourceHelper resourceHelper = new ResourceHelper(log, spotbugsXmlOutputDirectory, resourceManager)
        List<String> args = new ArrayList<>()

        if (userPrefs) {
            log.debug("  Adding User Preferences File -> ${userPrefs}")

            args << '-userPrefs'
            args << resourceHelper.getResourceFile(userPrefs.trim())
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

        File auxClasspathFile = createSpotbugsAuxClasspathFile()

        if (auxClasspathFile) {
            log.debug("  Adding 'auxclasspathFromFile'")
            args << '-auxclasspathFromFile'
            args << auxClasspathFile.getAbsolutePath()
        }

        log.debug("  Adding 'projectName'")
        args << '-projectName'
        args << "${project.name}"

        log.debug("  Adding 'effortParameter'")
        args << getEffortParameter()

        log.debug("  Adding 'thresholdParameter'")
        args << getThresholdParameter()

        if (debug) {
            log.debug("  Adding 'progress'")
            args << '-progress'
        }

        if (pluginList || plugins) {
            log.debug("  Adding 'pluginList'")
            args << '-pluginList'
            args << getSpotbugsPlugins()
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
            args << Arrays.stream(onlyAnalyze.split(SpotBugsInfo.COMMA)).map {
                it.startsWith('file:') ? Files.lines(resourceHelper.getResourceFile(it.substring(5)).toPath())
                    .collect(Collectors.joining(SpotBugsInfo.COMMA)) : it
            }.collect(Collectors.joining(','))
        }

        if (includeFilterFile) {
            log.debug('  Adding Include Filter File')

            String[] includefilters = includeFilterFile.split(SpotBugsInfo.COMMA)

            includefilters.each { includefilter ->
                args << '-include'
                args << resourceHelper.getResourceFile(includefilter.trim())
            }
        }

        if (includeFilterFiles) {
            log.debug('  Adding Include Filter Files')

            includeFilterFiles.each { includefilter ->
                args << '-include'
                args << resourceHelper.getResourceFile(includefilter.trim())
            }
        }

        if (excludeFilterFile) {
            log.debug('  Adding Exclude Filter File')
            String[] excludefilters = excludeFilterFile.split(SpotBugsInfo.COMMA)

            excludefilters.each { excludeFilter ->
                args << '-exclude'
                args << resourceHelper.getResourceFile(excludeFilter.trim())
            }
        }

        if (excludeFilterFiles) {
            log.debug('  Adding Exclude Filter Files')

            excludeFilterFiles.each { excludeFilter ->
                args << '-exclude'
                args << resourceHelper.getResourceFile(excludeFilter.trim())
            }
        }

        if (excludeBugsFile) {
            log.debug('  Adding Exclude Bug File (Baselines)')
            String[] excludeFiles = excludeBugsFile.split(SpotBugsInfo.COMMA)

            excludeFiles.each() { excludeFile ->
                args << '-excludeBugs'
                args << resourceHelper.getResourceFile(excludeFile.trim())
            }
        }

        if (excludeBugsFiles) {
            log.debug('  Adding Exclude Bug Files (Baselines)')

            excludeBugsFiles.each() { excludeFile ->
                args << '-excludeBugs'
                args << resourceHelper.getResourceFile(excludeFile.trim())
            }
        }

        if (addSourceDirs) {
            log.debug('  Adding Source directories (To process source exclusions)')
            args << '-sourcepath'
            String sourceRoots = ''
            session.getCurrentProject().compileSourceRoots.each() { sourceRoots += it + File.pathSeparator }
            if (includeTests) {
                session.getCurrentProject().testCompileSourceRoots.each() { sourceRoots += it + File.pathSeparator }
            }
            args << sourceRoots.substring(0, sourceRoots.length() -1)
        }

        if (maxRank) {
            log.debug("  Adding 'maxRank'")
            args << '-maxRank'
            args << maxRank
        }

        if (classFilesDirectory.isDirectory()) {
            log.debug('  Adding to Source Directory -> ' + classFilesDirectory.absolutePath)
            args << classFilesDirectory.absolutePath
        }

        if (testClassFilesDirectory.isDirectory() && includeTests) {
            log.debug('  Adding to Source Directory -> ' + testClassFilesDirectory.absolutePath)
            args << testClassFilesDirectory.absolutePath
        }

        if (noClassOk) {
            log.debug("  Adding 'noClassOk'")
            args << '-noClassOk'
        }

        return args
    }

    /**
     * Create the Spotbugs AuxClasspath file.
     *
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
            auxClasspathFile = File.createTempFile('auxclasspath', '.tmp')
            auxClasspathFile.deleteOnExit()
            log.debug('  AuxClasspath Elements -> ' + auxClasspathElements)

            List<String> auxClasspathList = auxClasspathElements.findAll {
                session.getCurrentProject().getBuild().outputDirectory != it.toString()
            }
            if (auxClasspathList.size() > 0) {
                log.debug('  Last AuxClasspath is -> ' + auxClasspathList[auxClasspathList.size() - 1])

                auxClasspathList.each() { auxClasspathElement ->
                    log.debug('  Adding to AuxClasspath -> ' + auxClasspathElement.toString())
                    auxClasspathFile << auxClasspathElement.toString() + SpotBugsInfo.EOL
                }
            }
        }

        return auxClasspathFile
    }

    /**
     * For the file creation by creating the file AND folder if needed.
     * The file created will be empty.
     *
     * @param file Destination file to create.
     */
    private void forceFileCreation(File file) {
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
            log.debug("Plugin Artifacts to be added -> ${pluginArtifacts.toString()}")
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

        log.info("Fork Value is ${fork}")

        long startTime
        if (log.isDebugEnabled()) {
            startTime = System.nanoTime()
        }

        List<String> spotbugsArgs = getSpotbugsArgs(htmlTempFile, xmlTempFile, sarifTempFile)

        Charset effectiveEncoding
        if (sourceEncoding) {
            effectiveEncoding = Charset.forName(sourceEncoding)
        } else {
            effectiveEncoding = Charset.defaultCharset() ?: StandardCharsets.UTF_8
        }
        log.debug('File Encoding is ' + effectiveEncoding.name())

        AntBuilder ant = new AntBuilder()
        ant.java(classname: 'edu.umd.cs.findbugs.FindBugs2', fork: "${fork}", failonerror: 'true',
                clonevm: 'false', timeout: timeout, maxmemory: "${maxHeap}m") {

            sysproperty(key: 'file.encoding', value: effectiveEncoding.name())

            if (jvmArgs && fork) {
                log.debug("Adding JVM Args => ${jvmArgs}")

                String[] args = jvmArgs.split(SpotBugsInfo.BLANK)

                args.each() { jvmArg ->
                    log.debug("Adding JVM Arg => ${jvmArg}")
                    jvmarg(value: jvmArg)
                }
            }

            if (debug || trace) {
                sysproperty(key: 'findbugs.debug', value: Boolean.TRUE)
            }

            classpath() {

                pluginArtifacts.each() { pluginArtifact ->
                    log.debug('  Adding to pluginArtifact -> ' + pluginArtifact.file.toString())

                    pathelement(location: pluginArtifact.file)
                }
            }

            spotbugsArgs.each { spotbugsArg ->
                log.debug("Spotbugs arg is ${spotbugsArg}")
                arg(value: spotbugsArg)
            }

            systemPropertyVariables.each { sysProp ->
                log.debug("System property ${sysProp.key} is ${sysProp.value}")
                sysproperty(key: sysProp.key, value: sysProp.value)
            }
        }

        long duration
        if (log.isDebugEnabled()) {
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
                GPathResult path = new XmlSlurper().parse(xmlTempFile)

                List<Node> allNodes = path.depthFirst().collect { it }

                bugCount = allNodes.findAll { it.name() == 'BugInstance' }.size()
                log.debug("BugInstance size is ${bugCount}")

                errorCount = allNodes.findAll { it.name() == 'Error' }.size()
                log.debug("Error size is ${errorCount}")

                NodeChildren xmlProject = path.Project

                session.getCurrentProject().compileSourceRoots.each() { compileSourceRoot ->
                    xmlProject.appendNode { SrcDir(compileSourceRoot) }
                }

                if (testClassFilesDirectory.isDirectory() && includeTests) {
                    session.getCurrentProject().testCompileSourceRoots.each() { testSourceRoot ->
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
                        effectiveEncoding.name().toLowerCase(Locale.ENGLISH) + '"?>'
                }

                writer.write SpotBugsInfo.EOL

                writer << xmlBuilder.bind { mkp.yield path }
            } else {
                log.info('No bugs found')
            }

            if (!log.isDebugEnabled()) {
                xmlTempFile.delete()
            }
        }

        if (sarifTempFile && sarifOutput && sarifTempFile.size() > 0) {

            Map slurpedResult = new JsonSlurper().parse(sarifTempFile)
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
                                log.info("${originalFullPath} modified to ${newFileName}")
                            }
                            loc.physicalLocation.artifactLocation.uri = newFileName
                        } else {
                            log.warn("No source file found for ${originalFullPath}. "
                                    + 'The path include in the SARIF report could be incomplete.')
                        }
                    }
                }
            }

            File sarifFinalFile = new File(sarifOutputDirectory, sarifOutputFilename)
            forceFileCreation(sarifFinalFile)

            sarifFinalFile.withWriter {
                builder.writeTo(it)
            }

            if (!log.isDebugEnabled()) {
                sarifTempFile.delete()
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

        log.debug("threshold is ${threshold}")

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
        log.debug("thresholdParameter is ${thresholdParameter}")

        return thresholdParameter
    }

    /**
     *  Set report output directory, allowed to be not read only as defined in AbstractMavenParent.
     *
     * @see AbstractMavenReport#setReportOutputDirectory(File)
     */
    @Override
    public void setReportOutputDirectory(File reportOutputDirectory) {
        super.setReportOutputDirectory(reportOutputDirectory)
        this.outputDirectory = reportOutputDirectory
    }
}
