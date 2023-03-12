/*
 * Copyright (C) 2006-2020 the original author or authors.
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

import groovy.json.JsonSlurper

def effortLevel = 'default'


File spotbugSarifFile = new File(basedir, 'target/spotbugsSarif.json')
assert spotbugSarifFile.exists()


println '**********************************'
println "Checking SARIF file"
println '**********************************'


def String normalizePath(String path) {
	return path.replaceAll("\\\\","/");
}

def slurpedResult = new JsonSlurper().parse(spotbugSarifFile)

def results = slurpedResult.runs.results[0]

for (result in slurpedResult.runs.results[0]) {

    for (loc in result.locations) {
        String location = normalizePath(loc.physicalLocation.artifactLocation.uri)
        //Making sure that the path was expanded
        assert location.contains("src/it-src/test/java") || location.contains("src/java") : "$location does not contain 'src/it-src/test/java'"
    }
}


println "BugInstance size is ${results.size()}"


assert results.size() > 0
