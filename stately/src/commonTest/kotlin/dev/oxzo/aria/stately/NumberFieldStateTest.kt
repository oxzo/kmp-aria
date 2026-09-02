package dev.oxzo.aria.stately

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NumberFieldStateTest {
    @Test
    fun snapValueToStepSnapsAndBounds() {
        assertEquals(1.5, snapValueToStep(1.3, 0.0, 10.0, 0.5))
        assertEquals(1.0, snapValueToStep(1.2, 0.0, 10.0, 0.5))
        assertEquals(0.0, snapValueToStep(-3.0, 0.0, 10.0, 1.0))
        // The grid from 0 by 3 overshoots 10; the last point below it is 9.
        assertEquals(9.0, snapValueToStep(11.0, 0.0, 10.0, 3.0))
        assertEquals(0.3, snapValueToStep(0.30000000000000004, null, null, 0.1))
    }

    @Test
    fun nextStepFromEmptyStartsAtTheBound() {
        assertEquals(0.0, nextStep(null, true, 0.0, 10.0, 1.0))
        assertEquals(10.0, nextStep(null, false, 0.0, 10.0, 1.0))
        assertEquals(0.0, nextStep(null, true, null, null, 1.0))
    }

    @Test
    fun nextStepSnapsBeforeItSteps() {
        // 1.3 is off the 0.5 grid; snapping up to 1.5 already moves up, so that is the step.
        assertEquals(1.5, nextStep(1.3, true, 0.0, 10.0, 0.5))
        // Snapping 1.3 down gives 1.0, which is the decrement.
        assertEquals(1.0, nextStep(1.3, false, 0.0, 10.0, 0.5))
        // On the grid, one full step.
        assertEquals(2.0, nextStep(1.5, true, 0.0, 10.0, 0.5))
        assertEquals(0.3, nextStep(0.2, true, null, null, 0.1))
    }

    @Test
    fun canStepStopsAtTheBounds() {
        assertFalse(canStep(10.0, true, 0.0, 10.0, 1.0))
        assertTrue(canStep(9.0, true, 0.0, 10.0, 1.0))
        assertFalse(canStep(0.0, false, 0.0, 10.0, 1.0))
        assertTrue(canStep(null, true, 0.0, 10.0, 1.0))
        assertTrue(canStep(10.0, true, null, null, 1.0))
    }

    @Test
    fun partialNumbersFollowTheBounds() {
        assertTrue(isValidPartialNumber(""))
        assertTrue(isValidPartialNumber("1,000.5"))
        assertTrue(isValidPartialNumber("-", min = -5.0))
        assertFalse(isValidPartialNumber("-", min = 0.0))
        assertTrue(isValidPartialNumber("+3", max = 10.0))
        assertFalse(isValidPartialNumber("1.2.3"))
        assertFalse(isValidPartialNumber("abc"))
        assertFalse(isValidPartialNumber("1.5", maximumFractionDigits = 0))
    }

    @Test
    fun parseAndFormatRoundTrip() {
        assertEquals(1024.0, parseNumber("1,024"))
        assertEquals(-3.5, parseNumber("-3.5"))
        assertEquals(0.5, parseNumber(".5"))
        assertNull(parseNumber(""))
        assertNull(parseNumber("-"))
        assertNull(parseNumber("Infinity"))
        assertEquals("1,024", formatNumber(1024.0))
        assertEquals("5", formatNumber(5.0))
        assertEquals("-3.5", formatNumber(-3.5))
        assertEquals("0.001", formatNumber(0.0005))
        assertEquals("1.235", formatNumber(1.23456))
    }

    @Test
    fun stateStepsCommitsAndNotifiesOnChangeOnly() {
        val seen = mutableListOf<Double?>()
        val state = NumberFieldState(initialValue = 5.0, minValue = 0.0, maxValue = 10.0, onChange = { seen += it })
        state.increment()
        assertEquals(6.0, state.numberValue)
        assertEquals("6", state.inputValue)
        state.incrementToMax()
        assertEquals(10.0, state.numberValue)
        assertFalse(state.canIncrement)
        state.increment()
        assertEquals(10.0, state.numberValue)
        state.setInputValue("42")
        assertEquals(42.0, state.parsedValue)
        state.commit()
        assertEquals(10.0, state.numberValue)
        assertEquals("10", state.inputValue)
        assertFalse(state.setInputValue("abc"))
        state.setInputValue("")
        state.commit()
        assertNull(state.numberValue)
        assertEquals(listOf(6.0, 10.0, null), seen)
    }

    @Test
    fun stateWithAStepSnapsTypedValues() {
        val state = NumberFieldState(initialValue = 1.0, minValue = 0.0, maxValue = 10.0, step = 0.5)
        state.setInputValue("1.3")
        state.commit()
        assertEquals(1.5, state.numberValue)
        assertEquals("1.5", state.inputValue)
        state.decrementToMin()
        assertEquals(0.0, state.numberValue)
        assertFalse(state.canDecrement)
    }
}
