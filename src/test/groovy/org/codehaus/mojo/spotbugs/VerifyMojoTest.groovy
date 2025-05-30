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

import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugins.annotations.Mojo
import spock.lang.Specification

class VerifyMojoTest extends Specification {

    void 'should extend BaseViolationCheckMojo'() {
        expect:
        BaseViolationCheckMojo.isAssignableFrom(VerifyMojo.class)
    }

    void 'should skip execution'() {
        given:
        Log log = Mock(Log)
        VerifyMojo mojo = new VerifyMojo()
        mojo.skip = true
        mojo.log = log

        when:
        mojo.execute()

        then:
        1 * log.debug('Executing spotbugs:check')
        1 * log.info('Spotbugs plugin skipped')
    }

}
