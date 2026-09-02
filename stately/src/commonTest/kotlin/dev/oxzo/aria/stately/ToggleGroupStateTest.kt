package dev.oxzo.aria.stately

import kotlin.test.Test
import kotlin.test.assertEquals

class ToggleGroupStateTest {
    @Test
    fun singleReplacesAndDeselects() {
        val seen = mutableListOf<Set<String>>()
        val state = ToggleGroupState(onChange = { seen += it })
        state.toggleKey("left")
        assertEquals(setOf("left"), state.selectedKeys)
        state.toggleKey("center")
        assertEquals(setOf("center"), state.selectedKeys)
        state.toggleKey("center")
        assertEquals(emptySet(), state.selectedKeys)
        assertEquals(listOf(setOf("left"), setOf("center"), emptySet()), seen)
    }

    @Test
    fun singleDisallowEmptyKeepsTheSelectedKey() {
        val state = ToggleGroupState(initialSelectedKeys = setOf("left"), disallowEmptySelection = true)
        state.toggleKey("left")
        assertEquals(setOf("left"), state.selectedKeys)
        state.toggleKey("right")
        assertEquals(setOf("right"), state.selectedKeys)
    }

    @Test
    fun multipleAccumulatesInPressOrder() {
        val state = ToggleGroupState(selectionMode = SelectionMode.Multiple)
        state.toggleKey("bold")
        state.toggleKey("italic")
        assertEquals(listOf("bold", "italic"), state.selectedKeys.toList())
        state.toggleKey("bold")
        assertEquals(setOf("italic"), state.selectedKeys)
    }

    @Test
    fun multipleDisallowEmptyKeepsTheLastKey() {
        val state = ToggleGroupState(selectionMode = SelectionMode.Multiple, disallowEmptySelection = true)
        state.toggleKey("bold")
        state.toggleKey("bold")
        assertEquals(setOf("bold"), state.selectedKeys)
        state.toggleKey("italic")
        state.toggleKey("bold")
        assertEquals(setOf("italic"), state.selectedKeys)
    }

    @Test
    fun setSelectedIsIdempotent() {
        val seen = mutableListOf<Set<String>>()
        val state = ToggleGroupState(selectionMode = SelectionMode.Multiple, onChange = { seen += it })
        state.setSelected("bold", true)
        state.setSelected("bold", true)
        state.setSelected("bold", false)
        state.setSelected("bold", false)
        assertEquals(listOf(setOf("bold"), emptySet()), seen)
    }
}
