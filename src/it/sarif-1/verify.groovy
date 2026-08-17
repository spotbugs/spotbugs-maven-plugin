/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import groovy.json.JsonSlurper

import java.nio.file.Files
import java.nio.file.Path

Path spotbugSarifFile = basedir.toPath().resolve('target/spotbugsSarif.json')
assert Files.exists(spotbugSarifFile)

println '*******************'
println 'Checking SARIF file'
println '*******************'

Map path = new JsonSlurper().parse(spotbugSarifFile)

List results = path.runs.results[0]
println "BugInstance size is ${results.size()}"

assert results.size() > 0
