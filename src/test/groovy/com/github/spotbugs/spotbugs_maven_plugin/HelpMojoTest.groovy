/*
 * Copyright 2005 the original author or authors.
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
package com.github.spotbugs.spotbugs_maven_plugin

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
        def field = obj.getClass().getDeclaredField(fieldName)
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
