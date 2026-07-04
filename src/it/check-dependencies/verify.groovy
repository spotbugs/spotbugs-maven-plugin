/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

Path spotbugXml = basedir.toPath().resolve('target/spotbugsXml.xml')
assert Files.exists(spotbugXml) : "SpotBugs XML output should have been generated"

Path spotbugsPluginsDir = basedir.toPath().resolve('target/spotbugs')
assert Files.isDirectory(spotbugsPluginsDir) : "Extension plugins should be copied into target/spotbugs/ subdirectory"

// Collect all JARs copied into target/spotbugs/
List<Path> copiedJars = Files.list(spotbugsPluginsDir).filter { p -> p.fileName.toString().endsWith('.jar') }.toList()
assert !copiedJars.isEmpty() : "target/spotbugs/ should contain the auto-detected extension plugin JARs from <dependencies>"

// Every JAR in target/spotbugs/ must be an actual SpotBugs extension plugin (contains findbugs.xml at root).
// This guards against SpotBugs core or other non-extension artifacts being copied there.
copiedJars.each { Path jar ->
    boolean hasDescriptor = false
    new JarFile(jar.toFile()).withCloseable { jf ->
        hasDescriptor = jf.getEntry('findbugs.xml') != null
    }
    assert hasDescriptor : "${jar.fileName} was copied to target/spotbugs/ but is not a SpotBugs extension plugin (no findbugs.xml found)"
}

// Verify the two expected extension plugins are present
assert copiedJars.any { it.fileName.toString().startsWith('sb-contrib') } :
    "sb-contrib JAR should have been auto-detected and copied"
assert copiedJars.any { it.fileName.toString().startsWith('findsecbugs-plugin') } :
    "findsecbugs-plugin JAR should have been auto-detected and copied"
