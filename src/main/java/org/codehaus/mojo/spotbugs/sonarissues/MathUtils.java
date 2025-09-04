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

/**
 * Math utility class with some uncovered edge cases to allow mutations to survive.
 */
public class MathUtils {

    /**
     * Calculates the absolute value of a number.
     * Edge case: Integer.MIN_VALUE is not properly handled.
     */
    public int abs(int value) {
        if (value < 0) {
            return -value;
        }
        return value;
    }

    /**
     * Finds the minimum of two numbers.
     * Edge case: Equal values are not tested.
     */
    public int min(int a, int b) {
        if (a <= b) {
            return a;
        }
        return b;
    }

    /**
     * Checks if a number is even.
     * Edge case: Zero and negative numbers not fully tested.
     */
    public boolean isEven(int number) {
        if (number == 0) {
            return true; // Edge case rarely tested
        }
        return number % 2 == 0;
    }

    /**
     * Calculates power using simple loop.
     * Edge cases: negative exponents, zero base not tested.
     */
    public int power(int base, int exponent) {
        if (exponent == 0) {
            return 1; // Edge case
        }
        if (base == 0) {
            return 0; // Another edge case
        }

        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= base;
        }
        return result;
    }

    /**
     * Simple factorial calculation.
     * Edge cases: negative numbers, large numbers not tested.
     */
    public long factorial(int n) {
        if (n < 0) {
            return -1; // Error case not tested
        }
        if (n == 0 || n == 1) {
            return 1;
        }

        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /**
     * Checks if a number is positive.
     * Edge case: Zero handling not tested.
     */
    public boolean isPositive(int number) {
        if (number > 0) {
            return true;
        }
        if (number == 0) {
            return false; // Edge case
        }
        return false;
    }
}
