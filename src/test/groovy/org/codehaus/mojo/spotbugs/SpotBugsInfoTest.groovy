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

    void "should have correct suffix and aggregate key constants"() {
        expect:
        SpotBugsInfo.JAR_SUFFIX == '.jar'
        SpotBugsInfo.ZIP_SUFFIX == '.zip'
        SpotBugsInfo.AGGREGATE_NAME_KEY == 'report.spotbugs.aggregate.name'
        SpotBugsInfo.AGGREGATE_DESCRIPTION_KEY == 'report.spotbugs.aggregate.description'
    }

    void "spotbugsThresholds contains exactly five entries"() {
        given:
        SpotBugsInfo info = new SpotBugsInfoImpl()

        expect:
        info.spotbugsThresholds.size() == 5
    }

    void "spotbugsEfforts contains exactly three entries"() {
        given:
        SpotBugsInfo info = new SpotBugsInfoImpl()

        expect:
        info.spotbugsEfforts.size() == 3
    }

    void "spotbugsPriority has unknown at index 0"() {
        given:
        SpotBugsInfo info = new SpotBugsInfoImpl()

        expect:
        info.spotbugsPriority[0] == 'unknown'
    }

    void "spotbugsPriority indexOf works correctly for valid priorities"() {
        given:
        SpotBugsInfo info = new SpotBugsInfoImpl()

        expect:
        info.spotbugsPriority.indexOf('High') == 1
        info.spotbugsPriority.indexOf('Medium') == 2
        info.spotbugsPriority.indexOf('Low') == 3
        info.spotbugsPriority.indexOf('NotAPriority') == -1
    }

}
