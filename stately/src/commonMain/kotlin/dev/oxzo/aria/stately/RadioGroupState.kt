package dev.oxzo.aria.stately

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Port of react-stately's `useRadioGroupState`: the selected value, the value that last held
 * focus (react-aria uses it to decide which radio is the group's tab stop when nothing is
 * selected), and the read-only guard. Snapshot-backed; no UI dependency.
 */
class RadioGroupState(
    initialValue: String? = null,
    val isReadOnly: Boolean = false,
    private val onChange: ((String?) -> Unit)? = null,
) {
    private var current: String? by mutableStateOf(initialValue)

    var selectedValue: String?
        get() = current
        set(value) {
            if (isReadOnly) return
            if (value == current) return
            current = value
            onChange?.invoke(value)
        }

    var lastFocusedValue: String? by mutableStateOf(null)
}
