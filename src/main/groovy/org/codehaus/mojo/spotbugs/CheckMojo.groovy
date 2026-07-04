/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package org.codehaus.mojo.spotbugs

import org.apache.maven.plugins.annotations.Execute
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 * Fail the build if there were any SpotBugs violations in the source code.
 * An XML report is put out by default in the target directory with the errors.
 * To see more documentation about SpotBugs' options,
 * please see the <a href="https://spotbugs.readthedocs.io/en/latest/" class="externalLink">SpotBugs Manual.</a>.
 *
 * @since 2.0
 */
@Mojo(name = 'check', defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST,
         requiresProject = true, threadSafe = true)
@Execute(goal = 'spotbugs')
class CheckMojo extends BaseViolationCheckMojo {
    // Check Mojo to run spotbugs execution
}
