package dev.oxzo.aria.stately

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToggleStateTest {
    @Test
    fun toggleFlipsAndNotifies() {
        val seen = mutableListOf<Boolean>()
        val state = ToggleState(onChange = { seen += it })
        assertFalse(state.isSelected)
        state.toggle()
        assertTrue(state.isSelected)
        state.toggle()
        assertFalse(state.isSelected)
        assertEquals(listOf(true, false), seen)
    }

    @Test
    fun readOnlyIgnoresWrites() {
        val state = ToggleState(initialSelected = true, isReadOnly = true)
        state.toggle()
        state.isSelected = false
        assertTrue(state.isSelected)
    }

    @Test
    fun sameValueDoesNotNotify() {
        var calls = 0
        val state = ToggleState(onChange = { calls++ })
        state.isSelected = false
        assertEquals(0, calls)
    }
}
