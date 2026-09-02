package dev.oxzo.aria.stately

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RadioGroupStateTest {
    @Test
    fun selectNotifies() {
        val seen = mutableListOf<String?>()
        val state = RadioGroupState(onChange = { seen += it })
        assertNull(state.selectedValue)
        state.selectedValue = "cat"
        assertEquals("cat", state.selectedValue)
        state.selectedValue = "cat"
        state.selectedValue = null
        assertEquals(listOf("cat", null), seen)
    }

    @Test
    fun readOnlyIgnoresWrites() {
        val state = RadioGroupState(initialValue = "dog", isReadOnly = true)
        state.selectedValue = "cat"
        assertEquals("dog", state.selectedValue)
    }

    @Test
    fun lastFocusedIsIndependentOfSelection() {
        val state = RadioGroupState(initialValue = "dog")
        state.lastFocusedValue = "cat"
        assertEquals("dog", state.selectedValue)
        assertEquals("cat", state.lastFocusedValue)
    }
}
