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

import org.apache.maven.plugin.AbstractMojo

import spock.lang.Specification

class BaseViolationCheckMojoTest extends Specification {

    static class TestMojo extends BaseViolationCheckMojo {
        @Override
        void execute() { /* no-op for test */ }
    }

    void 'should extend AbstractMojo'() {
        expect:
        AbstractMojo.isAssignableFrom(BaseViolationCheckMojo)
    }

    void 'should have default property values not injected'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.spotbugsXmlOutputFilename == null
        mojo.includeTests == false
        mojo.debug == false
        mojo.skip == false
        mojo.failOnError == false
        mojo.maxAllowedViolations == 0
        mojo.quiet == false
    }

    void 'should allow setting properties'() {
        given:
        TestMojo mojo = new TestMojo()

        when:
        mojo.spotbugsXmlOutputFilename = 'output.xml'
        mojo.includeTests = true
        mojo.debug = true
        mojo.skip = true
        mojo.failOnError = true
        mojo.maxAllowedViolations = 5
        mojo.quiet = true

        then:
        mojo.spotbugsXmlOutputFilename == 'output.xml'
        mojo.includeTests
        mojo.debug
        mojo.skip
        mojo.failOnError
        mojo.maxAllowedViolations == 5
        mojo.quiet
    }

    void 'should not throw when execute is called'() {
        given:
        TestMojo mojo = new TestMojo()

        when:
        mojo.execute()

        then:
        notThrown(Exception)
    }

}
