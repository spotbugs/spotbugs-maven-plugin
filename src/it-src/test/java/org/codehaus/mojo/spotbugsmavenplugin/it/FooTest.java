/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package org.codehaus.mojo.spotbugsmavenplugin.it;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

/**
 * Tests for {@link Foo}.
 *
 * @author user@example.com (John Doe)
 */
class FooTest {

    /**
     * This always passes.
     */
    @Test
    void thisAlwaysPasses() {
        // Do nothing
    }

    /**
     * This is ignored.
     */
    @Disabled
    @Test
    void thisIsIgnored() {
        // Do nothing
    }
}
