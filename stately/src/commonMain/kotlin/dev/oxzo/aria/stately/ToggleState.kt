package dev.oxzo.aria.stately

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Port of react-stately's `useToggleState`: a boolean selection with a read-only guard and a
 * change callback. Snapshot-backed so Compose observes it; no UI dependency.
 *
 * react-stately exposes `isSelected` + `setSelected()`; here the setter is the Kotlin
 * property setter (a separate `setSelected` function clashes with it on the JVM).
 */
class ToggleState(
    initialSelected: Boolean = false,
    val isReadOnly: Boolean = false,
    private val onChange: ((Boolean) -> Unit)? = null,
) {
    private var current: Boolean by mutableStateOf(initialSelected)

    var isSelected: Boolean
        get() = current
        set(value) {
            if (isReadOnly) return
            if (value == current) return
            current = value
            onChange?.invoke(value)
        }

    fun toggle() {
        isSelected = !isSelected
    }
}
