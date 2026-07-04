/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import java.nio.file.Files
import java.nio.file.Path

//  check module 1

Path spotbugXmlInModule = basedir.toPath().resolve('module1/src/main/config/spotbugs-exclude-filters.xml')
assert Files.notExists(spotbugXmlInModule)

//  check module 2

spotbugXmlInModule = basedir.toPath().resolve('module2/src/main/config/spotbugs-exclude-filters.xml')
assert Files.notExists(spotbugXmlInModule)
