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

import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 * Fail the build if any SpotBugs violations can be found in a preexisting {@code spotBugs.xml} file.
 * Note that unlike the {@code check} goal, this goal only reads existing XML reports, <b>without</b> actually performing SpotBugs analysis.
 * This way, it is possible to split the analysis and verification into lifecycle phases of one's choosing.
 * One use case for that is running multiple code analyzers at once and only failing the build at a later stage, so that all of them have a chance to run.
 * To see more documentation about SpotBugs' options, please see the <a href="https://spotbugs.readthedocs.io/en/latest/" class="externalLink">SpotBugs Manual.</a>.
 *
 * @since 4.7
 */
@Mojo( name = "verify", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true, threadSafe = true)
class VerifyMojo extends BaseViolationCheckMojo {
    // Verification Mojo to verify existing bugs rather than run spotbugs
}
