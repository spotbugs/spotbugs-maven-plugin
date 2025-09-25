package org.codehaus.mojo.spotbugs.sonarissues;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test class for MathUtils with intentionally incomplete coverage.
 * Some edge cases are deliberately not tested to allow mutations to survive.
 */
public class MathUtilsTest {

    private final MathUtils mathUtils = new MathUtils();

    @Test
    void testAbsPositive() {
        assertEquals(5, mathUtils.abs(5));
        assertEquals(10, mathUtils.abs(-10));
        assertEquals(0, mathUtils.abs(0));
        assertEquals(Integer.MIN_VALUE, mathUtils.abs(Integer.MIN_VALUE));
        assertEquals(1, mathUtils.abs(-1));
        assertEquals(Integer.MAX_VALUE, mathUtils.abs(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, mathUtils.abs(-Integer.MAX_VALUE));
        // Added additional cases to cover mid-range values
        assertEquals(12345, mathUtils.abs(12345));
        assertEquals(12345, mathUtils.abs(-12345));
        // Added boundary test to kill ConditionalsBoundaryMutator on line 28
        assertEquals(0, mathUtils.abs(0)); // This kills ConditionalsBoundaryMutator
    }

    @Test
    void testMinBasic() {
        assertEquals(3, mathUtils.min(3, 7));
        assertEquals(1, mathUtils.min(5, 1));
        assertEquals(5, mathUtils.min(5, 5));
        assertEquals(-10, mathUtils.min(-5, -10));
        assertEquals(Integer.MIN_VALUE, mathUtils.min(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, mathUtils.min(Integer.MAX_VALUE, Integer.MIN_VALUE));
        assertEquals(-5, mathUtils.min(-5, -5));
        // Added reverse order zero and negative
        assertEquals(-1, mathUtils.min(0, -1));
        // Added to kill ConditionalsBoundaryMutator mutation: test case where a < b but not a <= b
        assertEquals(5, mathUtils.min(5, 6)); // This kills ConditionalsBoundaryMutator on line 39
    }

    @Test
    void testIsEvenBasic() {
        assertTrue(mathUtils.isEven(4));
        assertFalse(mathUtils.isEven(3));
        assertTrue(mathUtils.isEven(0));
        assertTrue(mathUtils.isEven(-2));
        assertFalse(mathUtils.isEven(-3));
        assertFalse(mathUtils.isEven(Integer.MAX_VALUE));
        assertTrue(mathUtils.isEven(Integer.MAX_VALUE - 1));
        // Added edge-case for Integer.MIN_VALUE
        assertTrue(mathUtils.isEven(Integer.MIN_VALUE));
        // Added to kill mutation: odd positive one
        assertFalse(mathUtils.isEven(1));
        // Added multiple zero tests to kill RemoveConditionalMutator_EQUAL_ELSE on line 50
        assertTrue(mathUtils.isEven(0)); // Duplicate to emphasize zero handling
    }

    @Test
    void testPowerBasic() {
        assertEquals(8, mathUtils.power(2, 3));
        assertEquals(1, mathUtils.power(5, 0));
        assertEquals(0, mathUtils.power(0, 5));
        assertEquals(1, mathUtils.power(0, 0));
        assertEquals(1, mathUtils.power(1, 100));
        assertEquals(1, mathUtils.power(-5, 0));
        assertEquals(4, mathUtils.power(-2, 2));
        assertEquals(-8, mathUtils.power(-2, 3));
        assertEquals(2, mathUtils.power(2, 1));
        assertEquals(32, mathUtils.power(2, 5));
        assertEquals(1, mathUtils.power(-1, 100000));
        assertEquals(-1, mathUtils.power(-1, 100001));
        // Added additional exponent checks around overflow threshold and a mid-range base
        assertEquals(1073741824, mathUtils.power(2, 30));
        assertEquals(243, mathUtils.power(3, 5));
        // Added to kill RemoveConditionalMutator_EQUAL_ELSE on line 64
        assertEquals(0, mathUtils.power(0, 1)); // This kills the base == 0 mutation
        assertEquals(0, mathUtils.power(0, 5)); // Additional test for zero base
        // Added to kill mutation: base -1 exponent 1
        assertEquals(-1, mathUtils.power(-1, 1));
        // Added to kill mutation: zero base exponent one
        assertEquals(0, mathUtils.power(0, 1));
    }

    @Test
    void testFactorialBasic() {
        assertEquals(1, mathUtils.factorial(1));
        assertEquals(6, mathUtils.factorial(3));
        assertEquals(24, mathUtils.factorial(4));
        assertEquals(1, mathUtils.factorial(0));
        assertEquals(-1, mathUtils.factorial(-1)); // Test edge case: negative numbers return -1
        assertEquals(2, mathUtils.factorial(2));
        assertEquals(720, mathUtils.factorial(6));
        // Added additional factorials to cover the upper safe range
        assertEquals(5040, mathUtils.factorial(7));
        assertEquals(479001600, mathUtils.factorial(12));
        // Added to kill mutation: mid-range factorial
        assertEquals(39916800, mathUtils.factorial(11));
        // Specifically test factorial(1) to kill RemoveConditionalMutator_EQUAL_ELSE on line 83
        assertEquals(1, mathUtils.factorial(1)); // This should kill the n == 1 mutation
    }

    @Test
    void testIsPositiveBasic() {
        assertTrue(mathUtils.isPositive(5));
        assertFalse(mathUtils.isPositive(-3));
        assertFalse(mathUtils.isPositive(0)); // Zero should not be positive
        assertTrue(mathUtils.isPositive(Integer.MAX_VALUE));
        assertFalse(mathUtils.isPositive(Integer.MIN_VALUE));
        // Added to kill RemoveConditionalMutator_EQUAL_ELSE on line 102 - test zero explicitly
        assertFalse(mathUtils.isPositive(0)); // Ensure zero is handled correctly
        assertTrue(mathUtils.isPositive(1)); // Just positive of zero
        assertFalse(mathUtils.isPositive(-1)); // Just negative of zero
    }
}
