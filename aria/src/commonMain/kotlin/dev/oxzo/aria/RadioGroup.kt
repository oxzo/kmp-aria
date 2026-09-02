package dev.oxzo.aria

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import dev.oxzo.aria.interactions.spaceOnlyActivation

/** Receiver for [AriaRadio] inside an [AriaRadioGroup]. */
class AriaRadioGroupScope internal constructor(
    val selectedValue: String?,
    internal val onSelect: (String) -> Unit,
    internal val groupEnabled: Boolean,
    internal val readOnly: Boolean,
    internal val lastFocused: MutableState<String?>,
    internal val radios: SnapshotStateList<RadioEntry>,
)

internal class RadioEntry(var value: String, var enabled: Boolean, val focusRequester: FocusRequester)

/**
 * Port of react-aria-components `RadioGroup` + `Radio`. Contract: `radiogroup` labelled by
 * its visible label; each radio is `radio` with `checked`; Space selects the focused radio,
 * Enter does not; the arrow keys move focus to the next or previous enabled radio, wrapping,
 * and select it (`useRadioGroup`'s key handler: all four arrows work, Left/Right flip under
 * RTL unless the orientation is vertical); the group is a single tab stop.
 *
 * Tab stop: react-aria gives `tabIndex=0` to the selected radio, else the last focused one,
 * else all; the browser then treats same-name radios as one stop. Compose has no
 * focusable-but-not-tabbable state (`canFocus = false` also refuses programmatic focus), so
 * the port makes the last focused radio focusable, falling back to the selected one and then
 * the first enabled one before any focus. Deviation from the reference: after a programmatic
 * value change, the last focused radio stays the tab stop rather than the selected one.
 *
 * Reference contract: https://react-aria.adobe.com/RadioGroup
 */
@Composable
fun AriaRadioGroup(
    value: String?,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
    labelStyle: TextStyle = TextStyle.Default,
    content: @Composable AriaRadioGroupScope.() -> Unit,
) {
    val lastFocused = remember { mutableStateOf<String?>(null) }
    val radios = remember { mutableStateListOf<RadioEntry>() }
    val scope = AriaRadioGroupScope(value, onChange, enabled, readOnly, lastFocused, radios)
    val flipHorizontal = LocalLayoutDirection.current == LayoutDirection.Rtl && orientation != Orientation.Vertical
    val groupModifier = modifier
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val step = when (event.key) {
                Key.DirectionDown -> 1
                Key.DirectionUp -> -1
                Key.DirectionRight -> if (flipHorizontal) -1 else 1
                Key.DirectionLeft -> if (flipHorizontal) 1 else -1
                else -> return@onPreviewKeyEvent false
            }
            val candidates = radios.filter { it.enabled }
            val index = candidates.indexOfFirst { it.value == lastFocused.value }
            if (index < 0) return@onPreviewKeyEvent false
            val next = candidates[(index + step + candidates.size) % candidates.size]
            lastFocused.value = next.value
            next.focusRequester.requestFocus()
            if (!readOnly) onChange(next.value)
            true
        }
        .selectableGroup()
        .semantics {
            contentDescription = label
            if (invalid) error("Invalid")
        }
    val body: @Composable () -> Unit = {
        BasicText(label, style = labelStyle)
        scope.content()
    }
    if (orientation == Orientation.Vertical) {
        Column(modifier = groupModifier) { body() }
    } else {
        Row(modifier = groupModifier, verticalAlignment = Alignment.CenterVertically) { body() }
    }
}

/**
 * One radio. The accessible name is the content. `modifier` is applied to the selectable
 * node, after the focus plumbing, so a caller's `testTag` and focus marker land on the
 * node the reference's `<input type="radio">` corresponds to.
 */
@Composable
fun AriaRadioGroupScope.AriaRadio(
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val isEnabled = enabled && groupEnabled
    val entry = remember { RadioEntry(value, isEnabled, focusRequester) }
    SideEffect {
        entry.value = value
        entry.enabled = isEnabled
    }
    DisposableEffect(entry) {
        radios.add(entry)
        onDispose { radios.remove(entry) }
    }
    val selectedValue = selectedValue
    val firstEnabled = radios.firstOrNull { it.enabled }?.value
    val lastFocused = lastFocused
    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = when {
                    lastFocused.value != null -> lastFocused.value == value
                    selectedValue != null -> selectedValue == value
                    else -> firstEnabled == value
                }
            }
            .onFocusChanged { if (it.isFocused) lastFocused.value = value }
            .then(modifier)
            .spaceOnlyActivation()
            .selectable(
                selected = value == selectedValue,
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                role = Role.RadioButton,
                onClick = {
                    lastFocused.value = value
                    focusRequester.requestFocus()
                    if (!readOnly) onSelect(value)
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}
