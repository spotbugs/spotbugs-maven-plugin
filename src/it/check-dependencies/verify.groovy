/*
 * Copyright 2005-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
