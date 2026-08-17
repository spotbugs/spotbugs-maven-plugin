/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package test;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private App() {
        // Do not allow instantiation
    }

    public static void main(String[] args) {
        if (Objects.isNull(args)) {
            logger.warn("Input args is null");
        }
        logger.info("Hello World!");
    }
}
