/*
 * Copyright 2005-2024 the original author or authors.
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

import org.apache.maven.plugins.annotations.Execute
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 * Fail the build if there were any SpotBugs violations in the source code.
 * An XML report is put out by default in the target directory with the errors.
 * To see more documentation about SpotBugs' options, please see the <a href="https://spotbugs.readthedocs.io/en/latest/" class="externalLink">SpotBugs Manual.</a>.
 *
 * @since 2.0
 */
@Mojo(name = 'check', defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true, threadSafe = true)
@Execute(goal = 'spotbugs')
class CheckMojo extends BaseViolationCheckMojo {
    // Check Mojo to run spotbugs execution
}
