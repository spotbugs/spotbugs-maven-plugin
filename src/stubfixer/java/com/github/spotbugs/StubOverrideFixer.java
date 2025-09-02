/*
 * Copyright 2005-2025 the original author or authors.
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
package com.github.spotbugs;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
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
    private static final Set<String> GROOVY_METHODS;
    static {
        Set<String> tempSet = new HashSet<>();
        tempSet.add("getMetaClass");
        tempSet.add("setMetaClass");
        tempSet.add("invokeMethod");
        tempSet.add("getProperty");
        tempSet.add("setProperty");
        GROOVY_METHODS = Collections.unmodifiableSet(tempSet);
    }

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void main(String[] args) throws IOException {
        Path stubsDir = Paths.get(args[0]);
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
