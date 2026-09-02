package dev.oxzo.aria

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import dev.oxzo.aria.interactions.ariaToggleable
import dev.oxzo.aria.stately.SelectionMode
import dev.oxzo.aria.stately.toggleKey

/** Receiver for the grouped [AriaToggleButton] overload inside an [AriaToggleButtonGroup]. */
class AriaToggleButtonGroupScope internal constructor(
    val selectedKeys: Set<String>,
    val selectionMode: SelectionMode,
    internal val onToggle: (String) -> Unit,
    internal val groupEnabled: Boolean,
    internal val lastFocused: MutableState<String?>,
    internal val items: SnapshotStateList<ToggleItemEntry>,
)

internal class ToggleItemEntry(var id: String, var enabled: Boolean, val focusRequester: FocusRequester)

/**
 * Port of react-aria-components `ToggleButtonGroup`. Contract (`useToggleButtonGroup` over
 * `useToolbar`, `useToggleGroupState`): in single selection the group is `radiogroup` and each
 * item `radio` with `aria-checked`; in multiple selection the group is `toolbar` and each item a
 * `button` with `aria-pressed`. Enter, Space and click toggle the item in either mode (the items
 * are `<button>`s, so `usePress` accepts both keys). The arrow keys along the orientation move
 * focus to the next or previous enabled item without wrapping and without changing the selection
 * (`createFocusManager(ref)` is called with no `wrap`); the cross-axis arrows are not handled;
 * Left and Right flip under RTL. The group is one tab stop: Tab leaves it, and focus returning
 * to the group lands on the item that last held focus.
 *
 * Tab stop: react-aria keeps every item tabbable and jumps to the edge item on Tab, then lets the
 * browser leave; on the way back in, its focus handler restores the last focused child. The port
 * reproduces the observable contract with the [AriaRadioGroup] plumbing instead: only the last
 * focused item (initially the first enabled one) is focusable. Deviation: Shift+Tab into a group
 * nothing has focused yet lands on the first item here, on the last item there.
 *
 * Compose vocabulary: single mode sets `selectableGroup` on the group and `Role.RadioButton` +
 * `selected` on the items; multiple mode has no toolbar role to set, and items carry
 * `toggleableState` as [AriaToggleButton] does. `label` becomes the group's `contentDescription`
 * (the reference's `aria-label`).
 *
 * Reference contract: https://react-aria.adobe.com/ToggleButtonGroup
 */
@Composable
fun AriaToggleButtonGroup(
    selectedKeys: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: SelectionMode = SelectionMode.Single,
    disallowEmptySelection: Boolean = false,
    orientation: Orientation = Orientation.Horizontal,
    enabled: Boolean = true,
    label: String? = null,
    content: @Composable AriaToggleButtonGroupScope.() -> Unit,
) {
    val lastFocused = remember { mutableStateOf<String?>(null) }
    val items = remember { mutableStateListOf<ToggleItemEntry>() }
    val scope = AriaToggleButtonGroupScope(
        selectedKeys = selectedKeys,
        selectionMode = selectionMode,
        onToggle = { id -> onSelectionChange(toggleKey(selectedKeys, id, selectionMode, disallowEmptySelection)) },
        groupEnabled = enabled,
        lastFocused = lastFocused,
        items = items,
    )
    val horizontal = orientation == Orientation.Horizontal
    val flip = horizontal && LocalLayoutDirection.current == LayoutDirection.Rtl
    val groupModifier = modifier
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val step = when (event.key) {
                Key.DirectionRight -> if (horizontal) (if (flip) -1 else 1) else return@onPreviewKeyEvent false
                Key.DirectionLeft -> if (horizontal) (if (flip) 1 else -1) else return@onPreviewKeyEvent false
                Key.DirectionDown -> if (!horizontal) 1 else return@onPreviewKeyEvent false
                Key.DirectionUp -> if (!horizontal) -1 else return@onPreviewKeyEvent false
                else -> return@onPreviewKeyEvent false
            }
            val candidates = items.filter { it.enabled }
            val index = candidates.indexOfFirst { it.id == lastFocused.value }
            if (index < 0) return@onPreviewKeyEvent false
            // No wrap: at the edge the key is consumed (useToolbar preventDefaults) and focus stays.
            candidates.getOrNull(index + step)?.let { next ->
                lastFocused.value = next.id
                next.focusRequester.requestFocus()
            }
            true
        }
        .then(if (selectionMode == SelectionMode.Single) Modifier.selectableGroup() else Modifier)
        .semantics {
            if (label != null) contentDescription = label
            if (!enabled) disabled()
        }
    if (horizontal) {
        Row(modifier = groupModifier, verticalAlignment = Alignment.CenterVertically) { scope.content() }
    } else {
        Column(modifier = groupModifier) { scope.content() }
    }
}

/**
 * One item of an [AriaToggleButtonGroup], keyed by [id] (the reference's `id` prop matched
 * against `selectedKeys`). The accessible name is the content. `modifier` is applied to the
 * toggleable node, after the focus plumbing, so a caller's `testTag` and focus marker land on
 * the node the reference's `<button>` corresponds to.
 */
@Composable
fun AriaToggleButtonGroupScope.AriaToggleButton(
    id: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val isEnabled = enabled && groupEnabled
    val entry = remember { ToggleItemEntry(id, isEnabled, focusRequester) }
    SideEffect {
        entry.id = id
        entry.enabled = isEnabled
    }
    DisposableEffect(entry) {
        items.add(entry)
        onDispose { items.remove(entry) }
    }
    val selected = id in selectedKeys
    val firstEnabled = items.firstOrNull { it.enabled }?.id
    val lastFocused = lastFocused
    val activate = {
        lastFocused.value = id
        focusRequester.requestFocus()
        onToggle(id)
    }
    val focusPlumbing = Modifier
        .focusRequester(focusRequester)
        .focusProperties {
            canFocus = if (lastFocused.value != null) lastFocused.value == id else firstEnabled == id
        }
        .onFocusChanged { if (it.isFocused) lastFocused.value = id }
        .then(modifier)
    val toggling = when (selectionMode) {
        SelectionMode.Single -> focusPlumbing.selectable(
            selected = selected,
            interactionSource = interactionSource,
            indication = null,
            enabled = isEnabled,
            role = Role.RadioButton,
            onClick = activate,
        )
        SelectionMode.Multiple -> focusPlumbing.ariaToggleable(
            isSelected = selected,
            onChange = { activate() },
            enabled = isEnabled,
            interactionSource = interactionSource,
        )
    }
    Box(modifier = toggling, contentAlignment = Alignment.Center) {
        content()
    }
}
