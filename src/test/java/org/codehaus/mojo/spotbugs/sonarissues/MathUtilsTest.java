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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test class for MathUtils with intentionally incomplete coverage.
 * Some edge cases are deliberately not tested to allow mutations to survive.
 */
class MathUtilsTest {

    private final MathUtils mathUtils = new MathUtils();

    @Test
    void testAbsPositive() {
        assertEquals(5, mathUtils.abs(5));
        assertEquals(10, mathUtils.abs(-10));
    }

    @Test
    void testMinBasic() {
        assertEquals(3, mathUtils.min(3, 7));
        assertEquals(1, mathUtils.min(5, 1));
    }

    @Test
    void testIsEvenBasic() {
        assertTrue(mathUtils.isEven(4));
        assertFalse(mathUtils.isEven(3));
    }

    @Test
    void testPowerBasic() {
        assertEquals(8, mathUtils.power(2, 3));
        assertEquals(1, mathUtils.power(5, 0));
    }

    @Test
    void testFactorialBasic() {
        assertEquals(1, mathUtils.factorial(1));
        assertEquals(6, mathUtils.factorial(3));
        assertEquals(24, mathUtils.factorial(4));
    }

    @Test
    void testIsPositiveBasic() {
        assertTrue(mathUtils.isPositive(5));
        assertFalse(mathUtils.isPositive(-3));
    }
}
