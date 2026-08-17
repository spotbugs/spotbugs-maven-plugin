/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2005-2026 the original author or authors.
 */
package com.github.spotbugs

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class StubOverrideFixerTest extends Specification {

    @TempDir
    Path tempDir

    void "main adds @Override annotation to Groovy methods in stub files"() {
        given:
        String stubSource = """\
public class MyStub {
    public groovy.lang.MetaClass getMetaClass() { return null; }
    public void setMetaClass(groovy.lang.MetaClass mc) {}
    public Object invokeMethod(String name, Object args) { return null; }
    public Object getProperty(String name) { return null; }
    public void setProperty(String name, Object value) {}
    public void normalMethod() {}
}
"""
        Path stubFile = tempDir.resolve("MyStub.java")
        Files.write(stubFile, stubSource.getBytes(StandardCharsets.UTF_8))

        when:
        StubOverrideFixer.main(new String[]{ tempDir.toString() })

        then:
        String result = new String(Files.readAllBytes(stubFile), StandardCharsets.UTF_8)
        result.contains("@java.lang.Override")
        // All five Groovy methods should have @Override
        result.count("@java.lang.Override") == 5
        // normalMethod should not get @Override
        !result.contains("@java.lang.Override\n    public void normalMethod()")
    }

    void "main processes all .java files in directory recursively"() {
        given:
        String stubSource = """\
public class AnotherStub {
    public groovy.lang.MetaClass getMetaClass() { return null; }
}
"""
        Path subDir = tempDir.resolve("subpackage")
        Files.createDirectories(subDir)
        Path stubFile = subDir.resolve("AnotherStub.java")
        Files.write(stubFile, stubSource.getBytes(StandardCharsets.UTF_8))

        when:
        StubOverrideFixer.main(new String[]{ tempDir.toString() })

        then:
        String result = new String(Files.readAllBytes(stubFile), StandardCharsets.UTF_8)
        result.contains("@java.lang.Override")
    }

    void "main ignores non-.java files"() {
        given:
        Path txtFile = tempDir.resolve("readme.txt")
        Files.write(txtFile, "not java".getBytes(StandardCharsets.UTF_8))

        when:
        StubOverrideFixer.main(new String[]{ tempDir.toString() })

        then:
        // No exception thrown, non-java file untouched
        new String(Files.readAllBytes(txtFile), StandardCharsets.UTF_8) == "not java"
    }

    void "main does not add @Override to non-Groovy methods"() {
        given:
        String stubSource = """\
public class PlainStub {
    public void myMethod() {}
    public String toString() { return "plain"; }
}
"""
        Path stubFile = tempDir.resolve("PlainStub.java")
        Files.write(stubFile, stubSource.getBytes(StandardCharsets.UTF_8))

        when:
        StubOverrideFixer.main(new String[]{ tempDir.toString() })

        then:
        String result = new String(Files.readAllBytes(stubFile), StandardCharsets.UTF_8)
        !result.contains("@java.lang.Override")
    }
}
