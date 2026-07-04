/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package org.codehaus.mojo.spotbugs

import org.apache.maven.plugin.logging.Log

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
        1 * log.debug('Executing spotbugs mojo')
        1 * log.info('Spotbugs plugin skipped')
    }

}
