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

import org.codehaus.mojo.spotbugs.ResourceHelper
import org.codehaus.plexus.resource.ResourceManager
import org.apache.maven.plugin.logging.Log
import spock.lang.Specification

class ResourceHelperTest extends Specification {

    void "getResourceFile returns a file and logs debug info"() {
        given:
        Log log = Mock(Log)
        File outputDirectory = File.createTempDir()
        ResourceManager resourceManager = Mock(ResourceManager) {
            getResourceAsInputStream(_) >> new ByteArrayInputStream("test".bytes)
        }
        ResourceHelper helper = new ResourceHelper(log, outputDirectory, resourceManager)
        String resource = "test/path/resource.txt"

        when:
        File result = helper.getResourceFile(resource)

        then:
        result.exists()
        1 * log.debug('location is test/path')
        1 * log.debug('resource is test/path/resource.txt')

        cleanup:
        result?.delete()
        outputDirectory?.deleteDir()
    }

}
