/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package com.github.spotbugs;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class StubOverrideFixer.
 */
public class StubOverrideFixer {

    /** The logger. */
    private static final Logger logger = LoggerFactory.getLogger(StubOverrideFixer.class);

    /** The Constant GROOVY_METHODS. */
    private static final Set<String> GROOVY_METHODS = Set.of("getMetaClass", "setMetaClass", "invokeMethod",
            "getProperty", "setProperty");

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void main(String[] args) throws IOException {
        Path stubsDir = Path.of(args[0]);
        try (Stream<Path> stream = Files.walk(stubsDir)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(StubOverrideFixer::processStub);
        }
    }

    /**
     * Process stub.
     *
     * @param filePath the file path
     */
    private static void processStub(Path filePath) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(filePath);

            cu.findAll(MethodDeclaration.class).forEach(method -> {
                if (shouldHaveOverride(method)) {
                    method.addAnnotation("java.lang.Override");
                }
            });

            Files.write(filePath, cu.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException | ParseProblemException e) {
            logger.error("Error processing: {} - {}", filePath, e.getMessage());
        }
    }

    /**
     * Should have override.
     *
     * @param method the method
     * @return true, if successful
     */
    private static boolean shouldHaveOverride(MethodDeclaration method) {
        return GROOVY_METHODS.contains(method.getNameAsString());
    }

}
