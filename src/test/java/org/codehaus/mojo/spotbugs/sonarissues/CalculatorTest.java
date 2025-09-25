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
package org.codehaus.mojo.spotbugs.sonarissues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CalculatorTest {

    private Calculator calculator;

    @BeforeEach
    public void setUp() {
        calculator = new Calculator();
    }

    @Test
    public void testAdd() {
        assertEquals(5, calculator.add(2, 3));
    }

    @Test
    public void testSubtract() {
        assertEquals(1, calculator.subtract(3, 2));
    }

    @Test
    public void testMultiply() {
        assertEquals(6, calculator.multiply(2, 3));
    }

    @Test
    public void testDivide() {
        assertEquals(2.0, calculator.divide(4, 2), 0.001);

        assertThrows(IllegalArgumentException.class, () -> calculator.divide(5, 0));
    }



    @Test
    public void testMax() {
        assertEquals(5, calculator.max(3, 5));
        assertEquals(5, calculator.max(5, 3));
        assertEquals(5, calculator.max(5, 5));
    }





    @Test
    public void testIsPrime() {
        assertTrue(calculator.isPrime(7));
        assertFalse(calculator.isPrime(4));
        assertFalse(calculator.isPrime(1));
        assertFalse(calculator.isPrime(0));
        assertTrue(calculator.isPrime(2));
        assertFalse(calculator.isPrime(9));
    }



    @Test
    public void testFibonacci() {
        assertEquals(0, calculator.fibonacci(0));
        assertEquals(1, calculator.fibonacci(1));
        assertEquals(8, calculator.fibonacci(6));
        assertEquals(1, calculator.fibonacci(2));
        assertEquals(2, calculator.fibonacci(3));
        assertThrows(IllegalArgumentException.class, () -> calculator.fibonacci(-1));
    }

    @Test
    public void testGcd() {
        assertEquals(5, calculator.gcd(10, 15));
    }

    @Test
    public void testIsValidInteger() {
        assertTrue(calculator.isValidInteger("123"));
        assertFalse(calculator.isValidInteger("abc"));
        assertFalse(calculator.isValidInteger(null));
        assertFalse(calculator.isValidInteger(""));
        assertTrue(calculator.isValidInteger("-123"));
    }
}
