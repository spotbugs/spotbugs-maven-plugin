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

import org.codehaus.mojo.spotbugs.SpotBugsInfo
import spock.lang.Specification

class SpotBugsInfoTest extends Specification {

    // Dummy implementation to access instance properties
    static class SpotBugsInfoImpl implements SpotBugsInfo {}

    void "should have correct static constants"() {
        expect:
        SpotBugsInfo.PLUGIN_NAME == 'spotbugs'
        SpotBugsInfo.BUNDLE_NAME == 'spotbugs'
        SpotBugsInfo.NAME_KEY == 'report.spotbugs.name'
        SpotBugsInfo.DESCRIPTION_KEY == 'report.spotbugs.description'
        SpotBugsInfo.SOURCE_ROOT_KEY == 'report.spotbugs.sourceRoot'
        SpotBugsInfo.TEST_SOURCE_ROOT_KEY == 'report.spotbugs.testSourceRoot'
        SpotBugsInfo.JAVA_SOURCES_KEY == 'report.spotbugs.javasources'
        SpotBugsInfo.EXTENSIONS == ['class'] as String[]
        SpotBugsInfo.COMMA == ','
        SpotBugsInfo.URL_SEPARATOR == '/'
        SpotBugsInfo.BLANK == ' '
        SpotBugsInfo.PERIOD == '.'
        SpotBugsInfo.URL == 'url'
        SpotBugsInfo.CLASS_SUFFIX == '.class'
    }

    void "should have correct EOL"() {
        expect:
        SpotBugsInfo.EOL == System.lineSeparator()
    }

    void "should have correct spotbugsEfforts map"() {
        given:
        SpotBugsInfo info = new SpotBugsInfoImpl()

        expect:
        info.spotbugsEfforts == [Max: 'max', Min: 'min', Default: 'default']
    }

    void "should have correct spotbugsThresholds map"() {
        given:
        SpotBugsInfo info = new SpotBugsInfoImpl()

        expect:
        info.spotbugsThresholds == [High: 'high', Exp: 'experimental', Low: 'low', Medium: 'medium', Default: 'medium']
    }

    void "should have correct spotbugsPriority list"() {
        given:
        SpotBugsInfo info = new SpotBugsInfoImpl()

        expect:
        info.spotbugsPriority == ['unknown', 'High', 'Medium', 'Low']
    }

}
