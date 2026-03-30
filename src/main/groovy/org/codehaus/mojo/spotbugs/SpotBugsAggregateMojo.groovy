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

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChild

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.xml.sax.SAXException

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import org.apache.maven.reporting.AbstractMavenReport
import org.apache.maven.reporting.MavenReport

/**
 * Generates an aggregate SpotBugs report for multi-module projects when the site plugin is run.
 * The HTML report is generated from the individual SpotBugs XML results of each module.
 * Run {@code spotbugs:spotbugs} on each module before using this goal.
 *
 * @since 4.9.4.2
 */
@Mojo(name = 'spotbugs-aggregate', aggregator = true, requiresDependencyResolution = ResolutionScope.TEST,
        requiresProject = true, threadSafe = true)
class SpotBugsAggregateMojo extends AbstractMavenReport {

    /** Location where the generated HTML aggregate report will be created. */
    @Parameter(defaultValue = '${project.reporting.outputDirectory}', required = true)
    File outputDirectory

    /**
     * The file encoding to use when creating the HTML reports. If the property
     * <code>project.reporting.outputEncoding</code> is not set, utf-8 is used.
     *
     * @since 4.9.4.2
     */
    @Parameter(defaultValue = '${project.reporting.outputEncoding}', property = 'outputEncoding')
    String outputEncoding

    /** Threshold of minimum bug severity to report. Valid values are High, Default, Low, Ignore, and Exp (for experimental). */
    @Parameter(defaultValue = 'Default', property = 'spotbugs.threshold')
    String threshold

    /**
     * Effort of the bug finders. Valid values are Min, Default and Max.
     *
     * @since 4.9.4.2
     */
    @Parameter(defaultValue = 'Default', property = 'spotbugs.effort')
    String effort

    /** Turn on Spotbugs debugging. */
    @Parameter(defaultValue = 'false', property = 'spotbugs.debug')
    boolean debug

    /**
     * Skip entire check.
     *
     * @since 4.9.4.2
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.skip')
    boolean skip

    /**
     * Specifies the directory where the Spotbugs native XML output will be generated in each module.
     * The aggregate mojo looks for this filename in each reactor project's build directory.
     *
     * @since 4.9.4.2
     */
    @Parameter(defaultValue = 'spotbugsXml.xml', property = 'spotbugs.outputXmlFilename')
    String spotbugsXmlOutputFilename

    /**
     * Skip the Spotbugs HTML report generation if there are no violations found. Defaults to
     * <code>false</code>.
     *
     * @since 4.9.4.2
     */
    @Parameter(defaultValue = 'false', property = 'spotbugs.skipEmptyReport')
    boolean skipEmptyReport

    /** The resource bundle. */
    ResourceBundle bundle

    /**
     * Checks whether prerequisites for generating this report are given.
     *
     * @return true if the report can be generated, otherwise false
     * @see AbstractMavenReport#canGenerateReport()
     */
    @Override
    boolean canGenerateReport() {
        if (skip) {
            log.info('Spotbugs aggregate plugin skipped')
            return false
        }

        boolean anyResults = reactorProjects.any { MavenProject p ->
            File xmlFile = new File(p.build.directory, spotbugsXmlOutputFilename)
            xmlFile.exists() && xmlFile.size() > 0
        }

        if (!anyResults) {
            log.info('No SpotBugs XML results found in any reactor project. ' +
                'Run spotbugs:spotbugs on each module before generating the aggregate report.')
        }

        return anyResults
    }

    /**
     * Returns the plugins description for the "generated reports" overview page.
     *
     * @param locale the locale the report should be generated for
     * @return description of the report
     * @see MavenReport#getDescription(Locale)
     */
    @Override
    String getDescription(Locale locale) {
        return getBundle(locale).getString(SpotBugsInfo.AGGREGATE_DESCRIPTION_KEY)
    }

    /**
     * Returns the plugins name for the "generated reports" overview page and the menu.
     *
     * @param locale the locale the report should be generated for
     * @return name of the report
     * @see MavenReport#getName(Locale)
     */
    @Override
    String getName(Locale locale) {
        return getBundle(locale).getString(SpotBugsInfo.AGGREGATE_NAME_KEY)
    }

    /**
     * Returns report output file name, without the extension.
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
     * @return name of the generated page
     * @see {@link MavenReport#getOutputPath()}
     */
    @Override
    String getOutputPath() {
        return SpotBugsInfo.PLUGIN_NAME
    }

    /**
     * Executes the generation of the aggregate report.
     *
     * @param locale the wanted locale to generate the report, could be null.
     * @see AbstractMavenReport#executeReport(Locale)
     */
    @Override
    void executeReport(Locale locale) {
        log.debug('****** SpotBugsAggregateMojo executeReport *******')

        if (!canGenerateReport()) {
            return
        }

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new MojoExecutionException('Cannot create html output directory')
        }

        Charset effectiveEncoding = outputEncoding ?
            Charset.forName(outputEncoding) : StandardCharsets.UTF_8

        if (log.isDebugEnabled()) {
            log.debug("Output Directory is ${outputDirectory}")
            log.debug("Output Encoding is ${effectiveEncoding.name()}")
            log.debug("Reactor projects: ${reactorProjects*.name}")
        }

        File aggregatedXmlFile = buildAggregatedXml(effectiveEncoding)

        if (aggregatedXmlFile == null || !aggregatedXmlFile.exists()) {
            log.warn('No SpotBugs XML results could be aggregated.')
            return
        }

        XmlSlurper xmlSlurper = new XmlSlurper()
        xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
        xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

        GPathResult aggregatedResults = xmlSlurper.parse(aggregatedXmlFile)

        int bugCount = aggregatedResults.BugInstance.size()

        if (skipEmptyReport && bugCount == 0) {
            log.info('Skipping generation of SpotBugs aggregate HTML report since there are no bugs found.')
            return
        }

        log.debug('Generating SpotBugs aggregate HTML report')

        SpotbugsReportGenerator generator = new SpotbugsReportGenerator(getSink(), getBundle(locale))

        generator.setIsJXRReportEnabled(false)
        generator.setLog(log)
        generator.threshold = threshold
        generator.effort = effort
        generator.setSpotbugsResults(aggregatedResults)
        generator.setOutputDirectory(outputDirectory)
        generator.generateReport()

        if (log.isDebugEnabled()) {
            log.debug("Aggregate SpotBugs report generated with ${bugCount} bugs")
        }
    }

    /**
     * Collects and merges SpotBugs XML files from all reactor projects into a single aggregate XML file.
     *
     * @param effectiveEncoding the charset to use for writing the output file
     * @return the merged XML file, or null if no XML files were found
     */
    private File buildAggregatedXml(Charset effectiveEncoding) {
        XmlSlurper xmlSlurper = new XmlSlurper()
        xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)
        xmlSlurper.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

        List<GPathResult> allResults = []

        reactorProjects.each { MavenProject p ->
            File xmlFile = new File(p.build.directory, spotbugsXmlOutputFilename)
            if (xmlFile.exists() && xmlFile.size() > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding SpotBugs results from ${p.name}: ${xmlFile}")
                }
                try {
                    allResults.add(xmlSlurper.parse(xmlFile))
                } catch (SAXException | IOException e) {
                    log.warn("Failed to parse SpotBugs XML from ${xmlFile}: ${e.message}")
                }
            } else if (log.isDebugEnabled()) {
                log.debug("No SpotBugs XML found for module ${p.name} at ${xmlFile}")
            }
        }

        if (allResults.empty) {
            return null
        }

        // Collect all source directories from all modules
        List<String> allSrcDirs = []
        allResults.each { GPathResult result ->
            result.Project.SrcDir.each { srcDir ->
                String dir = srcDir.text()
                if (dir && !allSrcDirs.contains(dir)) {
                    allSrcDirs.add(dir)
                }
            }
        }

        // Count all bugs
        int totalBugs = allResults.sum(0) { GPathResult result ->
            result.BugInstance.size()
        }

        // Count total classes from FindBugsSummary attributes
        int totalClasses = allResults.sum(0) { GPathResult result ->
            String totalClassesStr = result.FindBugsSummary.@total_classes.text()
            totalClassesStr ? totalClassesStr.toInteger() : 0
        }

        // Sum error counts
        int totalErrors = allResults.sum(0) { GPathResult result ->
            String errorsStr = result.Errors.@errors.text()
            errorsStr ? errorsStr.toInteger() : 0
        }

        int totalMissingClasses = allResults.sum(0) { GPathResult result ->
            String missingStr = result.Errors.@missingClasses.text()
            missingStr ? missingStr.toInteger() : 0
        }

        if (log.isDebugEnabled()) {
            log.debug("Aggregating ${totalBugs} bugs, ${totalClasses} classes from ${allResults.size()} modules")
        }

        // Write merged XML file
        File outputDir = new File(project.build.directory)
        Files.createDirectories(outputDir.toPath())
        File aggregatedXmlFile = new File(outputDir, spotbugsXmlOutputFilename)

        StreamingMarkupBuilder xmlBuilder = new StreamingMarkupBuilder()
        xmlBuilder.encoding = effectiveEncoding.name()

        BufferedWriter writer = Files.newBufferedWriter(aggregatedXmlFile.toPath(), effectiveEncoding)

        if (effectiveEncoding.name().equalsIgnoreCase('Cp1252')) {
            writer.write '<?xml version="1.0" encoding="windows-1252"?>'
        } else {
            writer.write '<?xml version="1.0" encoding="' +
                effectiveEncoding.name().toLowerCase(Locale.getDefault()) + '"?>'
        }
        writer.write SpotBugsInfo.EOL

        def markup = xmlBuilder.bind { builder ->
            BugCollection {
                Project(name: project.name) {
                    allSrcDirs.each { String srcDir ->
                        SrcDir(srcDir)
                    }
                    WrkDir(project.build.directory)
                }
                allResults.each { GPathResult result ->
                    result.BugInstance.each { NodeChild bugInstance ->
                        mkp.yield bugInstance
                    }
                }
                Errors(errors: totalErrors, missingClasses: totalMissingClasses)
                FindBugsSummary(total_bugs: totalBugs, total_classes: totalClasses) {
                    allResults.each { GPathResult result ->
                        result.FindBugsSummary.PackageStats.each { NodeChild packageStats ->
                            mkp.yield packageStats
                        }
                    }
                }
            }
        }

        writer << markup
        writer.close()

        return aggregatedXmlFile
    }

    /**
     * Returns the report output directory.
     *
     * @return full path to the directory where the files in the site get copied to
     * @see AbstractMavenReport#getOutputDirectory()
     */
    @Override
    protected String getOutputDirectory() {
        return outputDirectory.absolutePath
    }

    /**
     * Sets the report output directory.
     *
     * @see AbstractMavenReport#setReportOutputDirectory(File)
     */
    @Override
    void setReportOutputDirectory(File reportOutputDirectory) {
        super.setReportOutputDirectory(reportOutputDirectory)
        this.outputDirectory = reportOutputDirectory
    }

    ResourceBundle getBundle(Locale locale) {
        this.bundle = ResourceBundle.getBundle(SpotBugsInfo.BUNDLE_NAME, locale,
            SpotBugsAggregateMojo.class.getClassLoader())

        if (log.isDebugEnabled()) {
            log.debug('Aggregate Mojo Locale is ' + this.bundle.getLocale().getLanguage())
        }

        return bundle
    }
}
