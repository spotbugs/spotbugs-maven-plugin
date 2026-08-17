/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import java.nio.file.Files

assert Files.notExists(basedir.toPath().resolve('target/site/spotbugs.html'))
assert Files.notExists(basedir.toPath().resolve('target/spotbugs.xml'))
assert Files.notExists(basedir.toPath().resolve('target/spotbugsXml.xml'))
