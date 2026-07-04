/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import java.nio.file.Files
import java.nio.file.Path

Path spotbugExclusionFile = basedir.toPath().resolve('module-1/target/spotbugs-exclude.xml')
assert Files.exists(spotbugExclusionFile)
