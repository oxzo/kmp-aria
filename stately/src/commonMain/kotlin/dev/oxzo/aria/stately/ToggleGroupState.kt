package dev.oxzo.aria.stately

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Single: at most one key selected (a radio group). Multiple: any number (a toolbar of toggles). */
enum class SelectionMode { Single, Multiple }

/**
 * The selection after toggling [key], as react-stately's `useToggleGroupState.toggleKey` computes
 * it: in multiple mode the key is removed if present (unless it is the last one and
 * [disallowEmptySelection]) and added otherwise; in single mode a selected key is deselected
 * (unless [disallowEmptySelection]) and any other key replaces the selection. Pure, so the
 * controlled composable and [ToggleGroupState] share one definition.
 */
fun toggleKey(
    selectedKeys: Set<String>,
    key: String,
    selectionMode: SelectionMode,
    disallowEmptySelection: Boolean = false,
): Set<String> = when (selectionMode) {
    SelectionMode.Multiple -> {
        val keys = selectedKeys.toMutableSet()
        if (key in keys && (!disallowEmptySelection || keys.size > 1)) keys.remove(key) else keys.add(key)
        keys
    }
    SelectionMode.Single ->
        if (key in selectedKeys && !disallowEmptySelection) emptySet() else setOf(key)
}

/**
 * Port of react-stately's `useToggleGroupState`: the selected keys of a toggle button group,
 * single or multiple selection, the disallow-empty guard and the disabled flag. Snapshot-backed;
 * no UI dependency.
 *
 * One difference from the hook: the change callback fires only when the set changes. The hook
 * rebuilds a `Set` on every toggle, so re-pressing the selected key under
 * `disallowEmptySelection` notifies there and not here.
 */
class ToggleGroupState(
    initialSelectedKeys: Set<String> = emptySet(),
    val selectionMode: SelectionMode = SelectionMode.Single,
    val disallowEmptySelection: Boolean = false,
    val isDisabled: Boolean = false,
    private val onChange: ((Set<String>) -> Unit)? = null,
) {
    private var current: Set<String> by mutableStateOf(initialSelectedKeys)

    var selectedKeys: Set<String>
        get() = current
        set(value) {
            if (value == current) return
            current = value
            onChange?.invoke(value)
        }

    fun toggleKey(key: String) {
        selectedKeys = toggleKey(current, key, selectionMode, disallowEmptySelection)
    }

    fun setSelected(key: String, isSelected: Boolean) {
        if (isSelected != key in current) toggleKey(key)
    }
}
