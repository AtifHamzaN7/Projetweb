package n7.facade;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdditionIATest {

    private final AdditionIA additionIA = new AdditionIA();

    @Test
    void testAdd() {
        assertEquals(5, additionIA.add(2, 3));
        assertEquals(-1, additionIA.add(-2, 1));
        assertEquals(0, additionIA.add(0, 0));
    }

    @Test
    void testAddThenSubtract() {
        // addThenSubtract(a,b,c) = (a+b) - c
        assertEquals(4, additionIA.addThenSubtract(3, 4, 3));
        assertEquals(0, additionIA.addThenSubtract(1, 1, 2));
        assertEquals(-5, additionIA.addThenSubtract(-2, -3, 0));
    }

    @Test
    void testAddThree() {
        assertEquals(6, additionIA.addThree(1, 2, 3));
        assertEquals(0, additionIA.addThree(0, 0, 0));
        assertEquals(-6, additionIA.addThree(-1, -2, -3));
    }

    @Test
    void testSum() {
        assertEquals(10, additionIA.sum(new int[]{1, 2, 3, 4}));
        assertEquals(0, additionIA.sum(new int[]{}));
        assertEquals(-10, additionIA.sum(new int[]{-1, -2, -3, -4}));
    }

    @Test
    void testSum_NullArray_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> additionIA.sum(null));
        assertEquals("values must not be null", ex.getMessage());
    }

    @Test
    void testSafeAdd() {
        assertEquals(5, additionIA.safeAdd(2, 3));
        assertEquals(-1, additionIA.safeAdd(-2, 1));
        assertEquals(0, additionIA.safeAdd(0, 0));
    }

    @Test
    void testSafeAdd_Overflow_Throws() {
        assertThrows(ArithmeticException.class, () -> additionIA.safeAdd(Integer.MAX_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> additionIA.safeAdd(Integer.MIN_VALUE, -1));
    }

    @Test
    void testRangeSum() {
        assertEquals(15, additionIA.rangeSum(1, 5));
        assertEquals(0, additionIA.rangeSum(0, 0));
        assertEquals(-15, additionIA.rangeSum(-5, -1));
    }

    @Test
    void testRangeSum_StartGreaterThanEnd_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> additionIA.rangeSum(5, 1));
        assertEquals("start must be <= end", ex.getMessage());
    }

    @Test
    void testAddAll() {
        assertEquals(10, additionIA.addAll(1, 2, 3, 4));
        assertEquals(0, additionIA.addAll());
        assertEquals(-10, additionIA.addAll(-1, -2, -3, -4));
    }

    @Test
    void testAddAll_NullArray_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> additionIA.addAll((int[]) null));
        assertEquals("values must not be null", ex.getMessage());
    }

    @Test
    void testIsSumEven() {
        assertTrue(additionIA.isSumEven(2, 2));
        assertTrue(additionIA.isSumEven(1, 3));
        assertFalse(additionIA.isSumEven(1, 2));
        assertFalse(additionIA.isSumEven(0, 1));
    }

    @Test
    void testClampAdd() {
        assertEquals(5, additionIA.clampAdd(2, 3, 0, 10));
        assertEquals(0, additionIA.clampAdd(-10, 0, 0, 10));
        assertEquals(10, additionIA.clampAdd(10, 10, 0, 10));
        assertEquals(7, additionIA.clampAdd(3, 4, 5, 10));
    }

    @Test
    void testClampAdd_MinGreaterThanMax_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> additionIA.clampAdd(1, 2, 10, 5));
        assertEquals("min must be <= max", ex.getMessage());
    }

}