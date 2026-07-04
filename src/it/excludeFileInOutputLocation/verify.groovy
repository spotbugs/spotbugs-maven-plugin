/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import java.nio.file.Files
import java.nio.file.Path

Path spotbugExclusionFile = basedir.toPath().resolve('target/generatedExclusionFile.xml')
assert Files.exists(spotbugExclusionFile)
assert Files.readString(spotbugExclusionFile) == '<?xml version="1.0" encoding="UTF-8"?><FindBugsFilter xmlns:pmd="http://pmd.sourceforge.net/ruleset/2.0.0"><Match><Or><Class name="~com.foo.bar.excluded..*"/></Or></Match></FindBugsFilter>'
