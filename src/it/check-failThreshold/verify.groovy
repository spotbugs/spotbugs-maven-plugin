/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import java.nio.file.Path
import java.nio.charset.StandardCharsets

Path buildLog = basedir.toPath().resolve('build.log')
assert buildLog.getText(StandardCharsets.UTF_8.name()).contains('[INFO] Medium: Unused public or protected field:')
assert buildLog.getText(StandardCharsets.UTF_8.name()).contains('[ERROR] High: Found reliance on default encoding in UserMistakes')
