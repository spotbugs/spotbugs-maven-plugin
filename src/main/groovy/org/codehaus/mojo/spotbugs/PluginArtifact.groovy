/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package org.codehaus.mojo.spotbugs

class PluginArtifact {

    /** The group id. */
    String groupId

    /** The artifact id. */
    String artifactId

    /** The version. */
    String version

    /** The type defaulted as jar. */
    String type = "jar"

    /** The classifier. */
    String classifier

}
