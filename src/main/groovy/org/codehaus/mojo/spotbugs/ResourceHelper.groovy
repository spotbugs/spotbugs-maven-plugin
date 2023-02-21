package org.codehaus.mojo.spotbugs

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.logging.Log
import org.codehaus.plexus.resource.loader.FileResourceCreationException
import org.codehaus.plexus.resource.loader.FileResourceLoader
import org.codehaus.plexus.resource.ResourceManager
import org.codehaus.plexus.util.FileUtils

final class ResourceHelper {

    Log log
    File outputDirectory
    ResourceManager resourceManager

    ResourceHelper(Log log, File outputDirectory, ResourceManager resourceManager) {
        assert log
        this.log = log
        this.outputDirectory = outputDirectory
        this.resourceManager = resourceManager
    }

    /**
     * Get the File reference for a File passed in as a string reference.
     *
     * @param resource
     *            The file for the resource manager to locate
     * @return The File of the resource
     *
     */
    File getResourceFile(String resource) {

        assert resource

        String location = null
        String artifact = resource

        // Linux Checks
        if (resource.indexOf(SpotBugsInfo.FORWARD_SLASH) != -1) {
            artifact = resource.substring(resource.lastIndexOf(SpotBugsInfo.FORWARD_SLASH) + 1)
        }

        if (resource.indexOf(SpotBugsInfo.FORWARD_SLASH) != -1) {
            location = resource.substring(0, resource.lastIndexOf(SpotBugsInfo.FORWARD_SLASH))
        }

        // Windows Checks
        if (resource.indexOf(SpotBugsInfo.BACKWARD_SLASH) != -1) {
            artifact = resource.substring(resource.lastIndexOf(SpotBugsInfo.BACKWARD_SLASH) + 1)
        }

        if (resource.indexOf(SpotBugsInfo.BACKWARD_SLASH) != -1) {
            location = resource.substring(0, resource.lastIndexOf(SpotBugsInfo.BACKWARD_SLASH))
        }

        // replace all occurrences of the following characters:  ? : & =
        location = location?.replaceAll("[\\?\\:\\&\\=\\%]", "_")
        artifact = artifact?.replaceAll("[\\?\\:\\&\\=\\%]", "_")

        log.debug("resource is " + resource)
        log.debug("location is " + location)
        log.debug("artifact is " + artifact)

        File resourceFile = getResourceAsFile(resource, artifact)

        log.debug("location of resourceFile file is " + resourceFile.absolutePath)

        return resourceFile

    }

    private File getResourceAsFile(String name, String outputPath) {
        // Optimization for File to File fetches
        File f = FileResourceLoader.getResourceAsFile(name, outputPath, outputDirectory)

        if (f != null) {
            return f
        }

        // End optimization

        File outputResourceFile

        if (outputPath == null) {
            outputResourceFile = FileUtils.createTempFile("plexus-resources", "tmp", null)
        } else {
            if (outputDirectory != null) {
                outputResourceFile = new File(outputDirectory, outputPath)
            } else {
                outputResourceFile = new File(outputPath)
            }
        }

        InputStream is = new BufferedInputStream(resourceManager.getResourceAsInputStream(name))

        try {
            if (!outputResourceFile.getParentFile().exists()) {
                outputResourceFile.getParentFile().mkdirs()
            }

            def os = new FileOutputStream(outputResourceFile)

            os << is

        } catch (IOException e) {
            throw new FileResourceCreationException("Cannot create file-based resource.", e)
        } finally {
            is.close()
        }

        return outputResourceFile
    }

}
