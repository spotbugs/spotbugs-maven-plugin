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
package org.codehaus.mojo.spotbugs

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern
import java.util.jar.JarFile

import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugin.MojoExecutionException
import org.codehaus.plexus.resource.ResourceManager
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResult

final class ResourceHelper {

    /** The log. */
    private final Log log

    /** The output directory. */
    private final File outputDirectory

    /** The resource manager. */
    private final ResourceManager resourceManager

    /** Artifact resolver for maven coordinates (optional). */
    private final org.eclipse.aether.RepositorySystem repositorySystem

    /** Maven session for repository context (optional). */
    private final MavenSession session

    /** Precompiled regex pattern for resource name sanitization. */
    private static final Pattern SANITIZE_PATTERN = Pattern.compile('[?:&=%]')

    /** Pattern for maven artifact resources in the format mvn:groupId:artifactId:version[:type[:classifier]]!/path/in/jar.xml. */
    private static final Pattern MAVEN_RESOURCE_PATTERN =
        Pattern.compile('^mvn:([^:]+):([^:]+):([^:!]+)(?::([^:!]+))?(?::([^:!]+))?!/(.+)$')

    ResourceHelper(final Log log, final File outputDirectory, final ResourceManager resourceManager) {
        this(log, outputDirectory, resourceManager, null, null)
    }

    ResourceHelper(final Log log, final File outputDirectory, final ResourceManager resourceManager,
            final org.eclipse.aether.RepositorySystem repositorySystem,
            final MavenSession session) {
        this.log = Objects.requireNonNull(log, "log must not be null")
        this.outputDirectory = outputDirectory
        this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager must not be null")
        this.repositorySystem = repositorySystem
        this.session = session
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
        String normalizedResource = resource.replace('\\', '/')
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
            log.debug("resource is '${normalizedResource}'" + ", location is '${location}'" +
                ", artifact is '${artifact}'" + ", outputDirectory is '${outputDirectory}'")
        }

        Path resourcePath = getResourceAsFile(normalizedResource, artifact)

        if (log.isDebugEnabled()) {
            log.debug("location of resourceFile file is ${resourcePath.toAbsolutePath()}")
        }

        return resourcePath.toFile()
    }

    private Path getResourceAsFile(final String name, final String outputPath) {
        Path outputResourcePath = outputDirectory == null ? Path.of(outputPath) : outputDirectory.toPath().resolve(outputPath)

        if (name.startsWith('mvn:')) {
            return resolveMavenResource(name, outputResourcePath)
        }

        // Checking if the resource is already a file
        if (new File(name).exists()) {
            // Avoid copying the file onto itself
            if (Path.of(name).toAbsolutePath().normalize().equals(outputResourcePath.toAbsolutePath().normalize())) {
                return outputResourcePath
            }

            createParentDirectories(outputResourcePath)

            // Copy existing file (not a URL)
            return Files.copy(Path.of(name), outputResourcePath, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES)
        }

        // Copying resource from classpath to a file
        try {
            createParentDirectories(outputResourcePath)

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

    private Path resolveMavenResource(final String name, final Path outputResourcePath) {
        if (repositorySystem == null || session == null) {
            throw new MojoExecutionException("Cannot resolve Maven resource '${name}': repository context is unavailable")
        }

        def matcher = MAVEN_RESOURCE_PATTERN.matcher(name)
        if (!matcher.matches()) {
            throw new MojoExecutionException("Invalid Maven resource syntax '${name}'. Use mvn:groupId:artifactId:version[:type[:classifier]]!/path/in/archive")
        }

        String groupId = matcher.group(1)
        String artifactId = matcher.group(2)
        String version = matcher.group(3)
        String type = matcher.group(4) ?: 'jar'
        String classifier = matcher.group(5) ?: ''
        String entryPath = matcher.group(6)

        def aetherArtifact = new DefaultArtifact(groupId, artifactId, classifier, type, version)
        ArtifactRequest request =
            new ArtifactRequest(aetherArtifact, session.getCurrentProject().getRemoteProjectRepositories(), null)
        ArtifactResult result = repositorySystem.resolveArtifact(session.getRepositorySession(), request)

        File artifactFile = result?.getArtifact()?.getFile()
        if (artifactFile == null || !artifactFile.exists()) {
            throw new MojoExecutionException("Resolved Maven artifact has no local file: ${groupId}:${artifactId}:${version}")
        }

        try {
            createParentDirectories(outputResourcePath)

            new JarFile(artifactFile).withCloseable { JarFile jar ->
                def entry = jar.getJarEntry(entryPath)
                if (entry == null || entry.isDirectory()) {
                    throw new MojoExecutionException("Entry '${entryPath}' not found in Maven artifact ${artifactFile.name}")
                }

                jar.getInputStream(entry).withCloseable { InputStream is ->
                    Files.copy(is, outputResourcePath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        } catch (IOException e) {
            log.error('Unable to create file-based resource for ' + name + ' in ' + outputResourcePath, e)
            throw new MojoExecutionException('Cannot create file-based resource.', e)
        }

        return outputResourcePath
    }

    private static createParentDirectories(Path outputResourcePath) {
        Path parent = outputResourcePath.getParent()
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent)
        }
    }

}
