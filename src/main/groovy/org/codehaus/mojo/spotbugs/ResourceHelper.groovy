/*
 * Copyright 2005-2025 the original author or authors.
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
package org.codehaus.mojo.spotbugs

import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugin.MojoExecutionException
import org.codehaus.plexus.resource.ResourceManager

final class ResourceHelper {

    /** The log. */
    private final Log log

    /** The output directory. */
    private final File outputDirectory

    /** The resource manager. */
    private final ResourceManager resourceManager

    /** Precompiled regex pattern for path separator normalization. */
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile('[\\\\]')

    /** Precompiled regex pattern for resource name sanitization. */
    private static final Pattern SANITIZE_PATTERN = Pattern.compile('[?:&=%]')

    ResourceHelper(final Log log, final File outputDirectory, final ResourceManager resourceManager) {
        this.log = Objects.requireNonNull(log, "log must not be null")
        this.outputDirectory = outputDirectory
        this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager must not be null")
    }

    /**
     * Get the File reference for a File passed in as a string reference.
     *
     * @param resource
     *            The file for the resource manager to locate
     * @return The File of the resource
     *
     */
    File getResourceFile(final String resource) {
        Objects.requireNonNull(resource, "resource must not be null")

        String location = null
        String artifact = null

        // Normalize path separator to always use forward slash for resource lookup
        String normalizedResource = SEPARATOR_PATTERN.matcher(resource).replaceAll('/')
        int lastSeparatorIndex = normalizedResource.lastIndexOf('/')
        if (lastSeparatorIndex != -1) {
            location = normalizedResource.substring(0, lastSeparatorIndex)
            artifact = normalizedResource.substring(lastSeparatorIndex + 1)
        } else {
            artifact = normalizedResource
        }

        // replace all occurrences of the following characters:  ? : & =
        location = location != null ? SANITIZE_PATTERN.matcher(location).replaceAll('_') : null
        artifact = SANITIZE_PATTERN.matcher(artifact).replaceAll('_')

        if (log.isDebugEnabled()) {
            log.debug("resource is ${normalizedResource}" + SpotBugsInfo.EOL + "location is ${location}" + SpotBugsInfo.EOL +
                "artifact is ${artifact}")
        }

        Path resourcePath = getResourceAsFile(normalizedResource, artifact)

        if (log.isDebugEnabled()) {
            log.debug("location of resourceFile file is ${resourcePath.toAbsolutePath()}")
        }

        return resourcePath.toFile()
    }

    private Path getResourceAsFile(final String name, final String outputPath) {
        Path outputResourcePath = outputDirectory == null ? Path.of(outputPath) : outputDirectory.toPath().resolve(outputPath)

        // If the resource already exists, just return it
        if (Path.of(name).toAbsolutePath().normalize().equals(outputResourcePath.toAbsolutePath().normalize())) {
            return outputResourcePath;
        }

        try {
            Path parent = outputResourcePath.getParent()
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent)
            }

            resourceManager.getResourceAsInputStream(name).withCloseable { InputStream is ->
                new BufferedInputStream(is).withCloseable { BufferedInputStream bis ->
                    Files.newOutputStream(outputResourcePath).withCloseable { OutputStream os ->
                        new BufferedOutputStream(os).withCloseable { BufferedOutputStream bos ->
                            bos << bis
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error('Unable to create file-based resource for ' + name + ' in ' + outputResourcePath, e)
            throw new MojoExecutionException('Cannot create file-based resource.', e)
        }

        return outputResourcePath
    }

}
