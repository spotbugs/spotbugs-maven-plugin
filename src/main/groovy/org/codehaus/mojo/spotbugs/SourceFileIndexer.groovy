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

import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Resource
import org.apache.maven.plugin.MojoExecutionException

import java.nio.file.Files
import java.nio.file.Path

/**
 * Utility class used to transform path relative to the <b>source directory</b>
 * to their path relative to the <b>root directory</b>.
 */
class SourceFileIndexer {

    /** List of source files found in the current Maven project. */
    private List<String> allSourceFiles

    /**
     * Initialize the complete list of source files with their
     *
     * @param project Reference to the Maven project to get the list of source directories
     * @param session Reference to the Maven session used to get the location of the root directory
     */
    protected void buildListSourceFiles(MavenSession session) {

        String basePath = normalizePath(session.getExecutionRootDirectory())

        List<String> allSourceFiles = []

        // Resource
        for (Resource r in session.getCurrentProject().getResources()) {
            scanDirectory(Path.of(r.directory), allSourceFiles, basePath)
        }

        for (Resource r in session.getCurrentProject().getTestResources()) {
            scanDirectory(Path.of(r.directory), allSourceFiles, basePath)
        }

        // Source files
        for (String sourceRoot in session.getCurrentProject().getCompileSourceRoots()) {
            scanDirectory(Path.of(sourceRoot), allSourceFiles, basePath)
        }

        for (String sourceRoot in session.getCurrentProject().getTestCompileSourceRoots()) {
            scanDirectory(Path.of(sourceRoot), allSourceFiles, basePath)
        }

        // While not perfect, add the following paths will add basic support for Groovy, Kotlin, Scala and Webapp sources.
        scanDirectory(session.getCurrentProject().getBasedir().toPath().resolve('src/main/groovy'), allSourceFiles, basePath)
        scanDirectory(session.getCurrentProject().getBasedir().toPath().resolve('src/main/kotlin'), allSourceFiles, basePath)
        scanDirectory(session.getCurrentProject().getBasedir().toPath().resolve('src/main/scala'), allSourceFiles, basePath)
        scanDirectory(session.getCurrentProject().getBasedir().toPath().resolve('src/main/webapp'), allSourceFiles, basePath)

        this.allSourceFiles = allSourceFiles
    }

    /**
     * Recursively scan the directory given and add all files found to the files array list.
     * The base directory will be truncated from the path stored.
     *
     * @param directory Directory to scan
     * @param files ArrayList where files found will be stored
     * @param baseDirectory This part will be truncated from path stored
     */
    private void scanDirectory(Path directory, List<String> files, String baseDirectory) {

        if (Files.notExists(directory)) {
            return
        }

        for (File child : directory.toFile().listFiles()) {
            if (child.isDirectory()) {
                scanDirectory(child.toPath(), files, baseDirectory)
            } else {
                String newSourceFile = normalizePath(child.getCanonicalPath())
                if (newSourceFile.startsWith(baseDirectory)) {
                    // The project will not be at the root of our file system.
                    // It will most likely be stored in a work directory.
                    // Example: /work/project-code-to-scan/src/main/java/File.java => src/main/java/File.java
                    //   (Here baseDirectory is /work/project-code-to-scan/)
                    String relativePath = Path.of(baseDirectory).relativize(Path.of(newSourceFile))
                    files.add(normalizePath(relativePath))
                } else {
                    // Use the full path instead:
                    // This will occurs in many cases including when the pom.xml is
                    // not in the same directory tree as the sources.
                    files.add(newSourceFile)
                }
            }
        }
    }

    /**
     * Normalize path to use forward slash.
     * <p>
     * This will facilitate searches.
     *
     * @param path Path to clean up
     * @return Path safe to use for comparison
     */
    private String normalizePath(String path) {
        return path.replace('\\\\','/')
    }

    /**
     * Transform partial path to complete path
     *
     * @param filename Partial name to search
     * @return Complete path found. Null is not found!
     */
    protected String searchActualFilesLocation(String filename) {

        if (allSourceFiles == null) {
            throw new MojoExecutionException('Source files cache must be built prior to searches.')
        }

        for (String fileFound in allSourceFiles) {
            if (fileFound.endsWith(filename)) {
                return fileFound
            }
        }

        // Not found
        return null
    }
}
