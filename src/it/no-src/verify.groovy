/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import java.nio.file.Files

assert Files.exists(basedir.toPath().resolve('target/site/index.html'))
assert Files.exists(basedir.toPath().resolve('target/spotbugs.xml'))
assert Files.exists(basedir.toPath().resolve('target/spotbugsXml.xml'))
