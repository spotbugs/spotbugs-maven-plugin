/*
 * Copyright 2005-2024 the original author or authors.
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


/**
 * Generates a Spotbugs Report when the site plugin is run.
 * The HTML report is generated for site commands only.
 * To see more documentation about Spotbugs' options, please see the
 * <a href="https://spotbugs.readthedocs.io/en/latest/">Spotbugs Manual.</a>
 */
interface SpotBugsInfo {

    /** The name of the Plug-In. */
    static final String PLUGIN_NAME = 'spotbugs'

    /** The name of the property resource bundle (Filesystem). */
    static final String BUNDLE_NAME = 'spotbugs'

    /** The key to get the name of the Plug-In from the bundle. */
    static final String NAME_KEY = 'report.spotbugs.name'

    /** The key to get the description of the Plug-In from the bundle. */
    static final String DESCRIPTION_KEY = 'report.spotbugs.description'

    /** The key to get the source directory message of the Plug-In from the bundle. */
    static final String SOURCE_ROOT_KEY = 'report.spotbugs.sourceRoot'

    /** The key to get the source directory message of the Plug-In from the bundle. */
    static final String TEST_SOURCE_ROOT_KEY = 'report.spotbugs.testSourceRoot'

    /** The key to get the java source message of the Plug-In from the bundle. */
    static final String JAVA_SOURCES_KEY = 'report.spotbugs.javasources'

    /** The extensions to search files for. */
    static final String[] EXTENSIONS = ['class']

    /** The comma character. */
    static final String COMMA = ','

    /** The forward slash character. */
    static final String FORWARD_SLASH = '/'

    /** The backward slash character. */
    static final String BACKWARD_SLASH = '\\'

    /** The character to separate URL tokens. */
    static final String URL_SEPARATOR = '/'

    /** The blank character. */
    static final String BLANK = ' '

    /** The period character. */
    static final String PERIOD = '.'

    /** The end of line separator. */
    static final EOL = System.lineSeparator()

    /** The url static string. */
    static final String URL = 'url'

    /** The static class suffix. */
    static final String CLASS_SUFFIX = '.class'

    /** The spotbugs efforts as max, min, and default as default. */
    Map<String, String> spotbugsEfforts = [Max: 'max', Min: 'min', Default: 'default']

    /** The spotbugs thresholds as high, experimental, low, medium, and default as medium. */
    Map<String, String> spotbugsThresholds = [High: 'high', Exp: 'experimental', Low: 'low', Medium: 'medium', Default: 'medium']

    /** The spotbugs priority as unknown, high, medium, and low. */
    List<String> spotbugsPriority = [
        'unknown',
        'High',
        'Medium',
        'Low'
    ]
}
