/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package org.codehaus.mojo.spotbugsmavenplugin.it;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class App implements Cloneable {

    /** The logger. */
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /**
     * Instantiates a new app.
     */
    private App() {
        // Do not allow instantiation
    }

    /**
     * Main.
     *
     * @param args the args
     */
    public static void main(String[] args) {
        if (Objects.isNull(args)) {
            logger.warn("Input args is null");
        }
        logger.info("Hello World!");
    }

    /**
     * Clone.
     *
     * @return the object
     */
    public Object clone() {
        return null; // Does not call 'super.clone()'.
    }

}
