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

import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Plugin
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.plugin.logging.Log
import org.apache.maven.toolchain.Toolchain
import org.apache.maven.toolchain.ToolchainManager

import spock.lang.Specification
import spock.lang.TempDir

class SpotBugsMojoTest extends Specification {

    @TempDir
    File tempDir

    void 'should extend AbstractMojo'() {
        expect:
        AbstractMojo.isAssignableFrom(SpotBugsMojo)
    }

    void 'should skip generate report'() {
        given:
        Log log = Mock(Log) {
            isDebugEnabled() >> true
        }
        MojoExecution mojoExecution = Mock(MojoExecution)
        Plugin plugin = Mock(Plugin)
        SpotBugsMojo mojo = new SpotBugsMojo()
        mojo.skip = true
        mojo.log = log
        mojo.mojoExecution = mojoExecution

        // Set plugin in mojoExecution
        mojoExecution.getPlugin() >> plugin

        when:
        mojo.execute()

        then:
        1 * log.info('Spotbugs plugin skipped')
    }

    void 'containsJdkClasses returns true for a JAR with java.* classes'() {
        given:
        SpotBugsMojo mojo = new SpotBugsMojo()
        mojo.log = Mock(Log)

        File jarWithJdkClasses = new File(tempDir, 'fake-jdk-api.jar')
        new JarOutputStream(new FileOutputStream(jarWithJdkClasses)).withCloseable { jos ->
            // Add a class file under java/lang/ to simulate a JavaCard-style API JAR
            jos.putNextEntry(new JarEntry('java/lang/Object.class'))
            Object.class.getResourceAsStream('/java/lang/Object.class').withCloseable { is ->
                jos.write(is.readAllBytes())
            }
            jos.closeEntry()
        }

        when:
        def method = SpotBugsMojo.class.getDeclaredMethod('containsJdkClasses', String.class)
        method.setAccessible(true)
        boolean result = method.invoke(mojo, jarWithJdkClasses.absolutePath)

        then:
        result == true
    }

    void 'containsJdkClasses returns false for a JAR without java.* classes'() {
        given:
        SpotBugsMojo mojo = new SpotBugsMojo()
        mojo.log = Mock(Log)

        File regularJar = new File(tempDir, 'regular-lib.jar')
        new JarOutputStream(new FileOutputStream(regularJar)).withCloseable { jos ->
            jos.putNextEntry(new JarEntry('com/example/SomeClass.class'))
            jos.write([0xCA, 0xFE, 0xBA, 0xBE] as byte[]) // minimal class-file magic bytes
            jos.closeEntry()
        }

        when:
        def method = SpotBugsMojo.class.getDeclaredMethod('containsJdkClasses', String.class)
        method.setAccessible(true)
        boolean result = method.invoke(mojo, regularJar.absolutePath)

        then:
        result == false
    }

    void 'containsJdkClasses returns false for a non-jar path'() {
        given:
        SpotBugsMojo mojo = new SpotBugsMojo()
        mojo.log = Mock(Log)

        File classesDir = new File(tempDir, 'target/classes')
        classesDir.mkdirs()

        when:
        def method = SpotBugsMojo.class.getDeclaredMethod('containsJdkClasses', String.class)
        method.setAccessible(true)
        boolean result = method.invoke(mojo, classesDir.absolutePath)

        then:
        result == false
    }

    void 'getJavaExecutable returns null when no toolchain is configured'() {
        given:
        MavenSession session = Mock(MavenSession)
        ToolchainManager toolchainManager = Mock(ToolchainManager) {
            getToolchainFromBuildContext('jdk', session) >> null
        }
        SpotBugsMojo mojo = new SpotBugsMojo()
        mojo.session = session
        mojo.toolchainManager = toolchainManager

        when:
        String result = mojo.getJavaExecutable()

        then:
        result == null
    }

    void 'getJavaExecutable returns null when toolchainManager is null'() {
        given:
        SpotBugsMojo mojo = new SpotBugsMojo()
        mojo.session = Mock(MavenSession)
        mojo.toolchainManager = null

        when:
        String result = mojo.getJavaExecutable()

        then:
        result == null
    }

    void 'getJavaExecutable returns java executable from configured toolchain'() {
        given:
        String expectedJavaPath = '/usr/lib/jvm/java-11/bin/java'
        MavenSession session = Mock(MavenSession)
        Toolchain toolchain = Mock(Toolchain) {
            findTool('java') >> expectedJavaPath
        }
        ToolchainManager toolchainManager = Mock(ToolchainManager) {
            getToolchainFromBuildContext('jdk', session) >> toolchain
        }
        SpotBugsMojo mojo = new SpotBugsMojo()
        mojo.session = session
        mojo.toolchainManager = toolchainManager

        when:
        String result = mojo.getJavaExecutable()

        then:
        result == expectedJavaPath
    }

}
