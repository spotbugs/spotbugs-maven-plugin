/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets

Path buildLog = basedir.toPath().resolve('build.log')
assert Files.exists(buildLog)

String log = buildLog.getText(StandardCharsets.UTF_8.name())
assert log.contains('Toolchain in spotbugs-maven-plugin:')

Path spotbugXml = basedir.toPath().resolve('target/spotbugsXml.xml')
assert Files.exists(spotbugXml)
