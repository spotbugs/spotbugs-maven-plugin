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
 * Calculator class with various methods to demonstrate mutation testing.
 * This class intentionally has gaps in test coverage to allow mutations to survive.
 */
public class Calculator {

    /**
     * Adds two numbers.
     */
    public int add(int a, int b) {
        return a + b;
    }

    /**
     * Subtracts two numbers.
     */
    public int subtract(int a, int b) {
        return a - b;
    }

    /**
     * Multiplies two numbers.
     */
    public int multiply(int a, int b) {
        return a * b;
    }

    /**
     * Divides two numbers with basic error handling.
     */
    public double divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("Division by zero");
        }
        return (double) a / b;
    }





    /**
     * Finds the maximum of two numbers.
     */
    public int max(int a, int b) {
        if (a > b) {
            return a;
        }
        return b;
    }





    /**
     * Checks if a number is prime.
     * This method has complex logic with multiple branches.
     */
    public boolean isPrime(int number) {
        if (number <= 1) {
            return false;
        }
        if (number <= 3) {
            return true;
        }
        if (number % 2 == 0 || number % 3 == 0) {
            return false;
        }

        for (int i = 5; i * i <= number; i += 6) {
            if (number % i == 0 || number % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }





    /**
     * Calculates Fibonacci number at position n.
     * Uses iterative approach to avoid stack overflow.
     */
    public long fibonacci(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Fibonacci of negative number");
        }
        if (n == 0) {
            return 0;
        }
        if (n == 1) {
            return 1;
        }

        long prev = 0;
        long curr = 1;

        for (int i = 2; i <= n; i++) {
            long next = prev + curr;
            prev = curr;
            curr = next;
        }

        return curr;
    }



    /**
     * Checks if a string represents a valid integer.
     */
    public boolean isValidInteger(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        str = str.trim();

        // Check for sign
        int startIndex = 0;
        if (str.charAt(0) == '+' || str.charAt(0) == '-') {
            if (str.length() == 1) {
                return false;
            }
            startIndex = 1;
        }

        // Check all characters are digits
        for (int i = startIndex; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates the greatest common divisor using Euclidean algorithm.
     * Has multiple edge cases and boundary conditions.
     */
    public int gcd(int a, int b) {
        // Handle edge cases
        if (a == 0 && b == 0) {
            return 1; // Mathematical convention, but creates edge case
        }
        if (a == 0) {
            return Math.abs(b);
        }
        if (b == 0) {
            return Math.abs(a);
        }

        a = Math.abs(a);
        b = Math.abs(b);

        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }

        return a;
    }
}
