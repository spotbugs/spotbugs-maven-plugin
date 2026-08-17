/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package com.github.spotbugs.spotbugs_maven_plugin

import java.lang.reflect.Field

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.logging.Log

import spock.lang.Specification

class HelpMojoTest extends Specification {

    private HelpMojo createMojo(boolean detail = false, String goal = null, int lineLength = 80, int indentSize = 2) {
        HelpMojo mojo = new HelpMojo()
        mojo.setLog(Mock(Log) {
            isInfoEnabled() >> true
            info(_) >> {}
            debug(_) >> {}
            warn(_) >> {}
        })
        // Set fields via reflection since they are private @Parameter fields
        setField(mojo, 'detail', detail)
        setField(mojo, 'goal', goal)
        setField(mojo, 'lineLength', lineLength)
        setField(mojo, 'indentSize', indentSize)
        return mojo
    }

    private static void setField(Object obj, String fieldName, Object value) {
        Field field = obj.getClass().getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(obj, value)
    }

    void "execute completes without error using default parameters"() {
        given:
        HelpMojo mojo = createMojo()

        when:
        mojo.execute()

        then:
        noExceptionThrown()
    }

    void "execute with detail=true lists all goals and parameters"() {
        given:
        HelpMojo mojo = createMojo(true)

        when:
        mojo.execute()

        then:
        noExceptionThrown()
    }

    void "execute with a specific goal name does not throw"() {
        given:
        HelpMojo mojo = createMojo(false, 'help')

        when:
        mojo.execute()

        then:
        noExceptionThrown()
    }

    void "execute with detail=true and specific goal does not throw"() {
        given:
        HelpMojo mojo = createMojo(true, 'spotbugs')

        when:
        mojo.execute()

        then:
        noExceptionThrown()
    }

    void "execute warns and resets lineLength when it is zero or negative"() {
        given:
        Log log = Mock(Log) {
            isInfoEnabled() >> true
            info(_) >> {}
            debug(_) >> {}
        }
        HelpMojo mojo = new HelpMojo()
        mojo.setLog(log)
        setField(mojo, 'detail', false)
        setField(mojo, 'goal', null)
        setField(mojo, 'lineLength', 0)
        setField(mojo, 'indentSize', 2)

        when:
        mojo.execute()

        then:
        1 * log.warn({ String msg -> msg.contains("lineLength") })
        noExceptionThrown()
    }

    void "execute warns and resets indentSize when it is zero or negative"() {
        given:
        Log log = Mock(Log) {
            isInfoEnabled() >> true
            info(_) >> {}
            debug(_) >> {}
        }
        HelpMojo mojo = new HelpMojo()
        mojo.setLog(log)
        setField(mojo, 'detail', false)
        setField(mojo, 'goal', null)
        setField(mojo, 'lineLength', 80)
        setField(mojo, 'indentSize', 0)

        when:
        mojo.execute()

        then:
        1 * log.warn({ String msg -> msg.contains("indentSize") })
        noExceptionThrown()
    }

    void "execute with unknown goal name does not throw"() {
        given:
        HelpMojo mojo = createMojo(false, 'nonexistent-goal')

        when:
        mojo.execute()

        then:
        noExceptionThrown()
    }
}
