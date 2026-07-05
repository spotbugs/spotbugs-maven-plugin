/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package org.codehaus.mojo.spotbugs

import com.codebox.bean.JavaBeanTester

import spock.lang.Specification

class PluginArtifactTest extends Specification {

    void "should satisfy JavaBean contract"() {
        expect:
        JavaBeanTester.builder(PluginArtifact.class).skip("metaClass").test()
    }

}
