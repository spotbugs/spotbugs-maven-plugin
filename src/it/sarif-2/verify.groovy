/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import groovy.json.JsonSlurper

import java.nio.file.Files
import java.nio.file.Path

String normalizePath(String path) {
    return path.replace('\\', '/')
}

Path spotbugSarifFile = basedir.toPath().resolve('target/spotbugsSarif.json')
assert Files.exists(spotbugSarifFile)

println '*******************'
println 'Checking SARIF file'
println '*******************'

Map slurpedResult = new JsonSlurper().parse(spotbugSarifFile)

List results = slurpedResult.runs.results[0]

for (result in results) {
    for (loc in result.locations) {
        String location = normalizePath(loc.physicalLocation.artifactLocation.uri)
        //Making sure that the path was expanded
        assert location.contains('src/it-src/test/java') || location.contains('src/java') : "${location} does not contain 'src/it-src/test/java' or 'src/java'"
    }
}

println "BugInstance size is ${results.size()}"

assert results.size() > 0
