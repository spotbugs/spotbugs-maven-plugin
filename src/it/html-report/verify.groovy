/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import java.nio.file.Files
import java.nio.file.Path

Path spotbugsHtml = basedir.toPath().resolve('target/test-output-directory/spotbugs.html')
assert Files.exists(spotbugsHtml)
