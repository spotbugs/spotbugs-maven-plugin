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
    }

    @Test
    void testPowerBasic() {
        assertEquals(8, mathUtils.power(2, 3));
        assertEquals(1, mathUtils.power(5, 0));
        assertThrows(IllegalArgumentException.class, () -> mathUtils.power(2, -1));
        assertEquals(0, mathUtils.power(0, 5));
        assertEquals(1, mathUtils.power(0, 0));
        assertEquals(1, mathUtils.power(1, 100));
        assertThrows(IllegalArgumentException.class, () -> mathUtils.power(0, -5));
        assertEquals(4, mathUtils.power(-2, 2));
        assertEquals(-8, mathUtils.power(-2, 3));
        assertEquals(2, mathUtils.power(2, 1));
        assertEquals(32, mathUtils.power(2, 5));
    }

    @Test
    void testFactorialBasic() {
        assertEquals(1, mathUtils.factorial(1));
        assertEquals(6, mathUtils.factorial(3));
        assertEquals(24, mathUtils.factorial(4));
        assertEquals(1, mathUtils.factorial(0));
        assertThrows(IllegalArgumentException.class, () -> mathUtils.factorial(-1));
        assertEquals(2, mathUtils.factorial(2));
        assertEquals(720, mathUtils.factorial(6));
        assertThrows(ArithmeticException.class, () -> mathUtils.factorial(13));
        assertThrows(ArithmeticException.class, () -> mathUtils.factorial(14));
    }

    @Test
    void testIsPositiveBasic() {
        assertTrue(mathUtils.isPositive(5));
        assertFalse(mathUtils.isPositive(-3));
        assertFalse(mathUtils.isPositive(0));
        assertTrue(mathUtils.isPositive(Integer.MAX_VALUE));
        assertFalse(mathUtils.isPositive(Integer.MIN_VALUE));
    }
}
