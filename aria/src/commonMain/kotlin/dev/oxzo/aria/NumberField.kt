package dev.oxzo.aria

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.oxzo.aria.stately.canStep
import dev.oxzo.aria.stately.clamp
import dev.oxzo.aria.stately.formatNumber
import dev.oxzo.aria.stately.isValidPartialNumber
import dev.oxzo.aria.stately.nextStep
import dev.oxzo.aria.stately.parseNumber
import dev.oxzo.aria.stately.snapValueToStep

/**
 * Port of react-aria-components `NumberField` (`Label` + `Group` of a decrement `Button`, the
 * `Input` and an increment `Button`). Contract (`useNumberField`, `useNumberFieldState`,
 * `useSpinButton`): the input is a plain `textbox` named by the label; the hook computes a
 * spinbutton role and the four `aria-value*` attributes and then nulls them on the input ("we
 * can't focus a spin button with VO"), describing the role as "Number field" through
 * `aria-roledescription` instead. Typing is filtered to partial numbers; ArrowUp/ArrowDown and
 * PageUp/PageDown step by [step] (there is no page step), Home/End jump to the bounds; Enter and
 * focus loss commit (parse, clamp, snap to the step, reformat). The two buttons are named
 * "Increase <label>" / "Decrease <label>", sit outside the tab order, are disabled at the bounds,
 * and a mouse press on them moves focus to the input. The wrapper is `role="group"` without a
 * name, `aria-disabled` and `aria-invalid` when so. Disabled or read-only turns the shortcuts and
 * the buttons off.
 *
 * Compose vocabulary: `contentDescription` names the field and the buttons; the group is a `Row`
 * with a semantics node and no role (Compose has none); `KeyboardType.Number` stands in for
 * `inputMode="numeric"`, and on the web target it lands on the backing `<input>` as
 * `inputmode="number"` (`DomInputStrategy.kt` line 209, 1.12.0), which is not one of the eight
 * `inputmode` keywords (`numeric` is), so the soft-keyboard hint is lost there. Compose has no
 * focusable-but-untabbable state, so the buttons are unfocusable, as [AriaSearchField]'s clear
 * button is. Not ported: the assertive live announcement of the value on change, press-and-hold
 * spinning, the scroll wheel, locale-aware formatting (`en-US` grouping and up to three decimals,
 * see [formatNumber]), `aria-controls`, `aria-roledescription`, the hidden form input.
 *
 * Reference contract: https://react-aria.adobe.com/NumberField
 */
@Composable
fun AriaNumberField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minValue: Double? = null,
    maxValue: Double? = null,
    step: Double? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
    description: String? = null,
    errorMessage: String? = null,
    textStyle: TextStyle = TextStyle.Default,
    interactionSource: MutableInteractionSource? = null,
    groupModifier: Modifier = Modifier,
    incrementModifier: Modifier = Modifier,
    decrementModifier: Modifier = Modifier,
    incrementButton: @Composable () -> Unit = { BasicText("+", style = textStyle) },
    decrementButton: @Composable () -> Unit = { BasicText("-", style = textStyle) },
) {
    val clampStep = step ?: 1.0
    var inputText by remember { mutableStateOf(value?.let { formatNumber(it) } ?: "") }
    var syncedValue by remember { mutableStateOf(value) }
    if (value != syncedValue) {
        // The controlled value changed from outside: reformat the text, as the hook's
        // prevValue check does.
        inputText = value?.let { formatNumber(it) } ?: ""
        syncedValue = value
    }
    val active = enabled && !readOnly
    // Parsed at call time, never captured at composition: on the web target the backing input's
    // keystrokes and the non-typed keys are replayed together at the next animation frame
    // (NativeInputEventsProcessor.runCheckpoint, 1.12.0), so an Enter typed right after a digit
    // reaches commit() before recomposition, and a value captured at composition would be a
    // frame stale (measured in session 5: the digit was dropped).
    fun parsed(): Double? = parseNumber(inputText)
    val inputFocus = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }

    fun setValue(v: Double?) {
        inputText = v?.let { formatNumber(it) } ?: ""
        syncedValue = v
        if (v != value) onValueChange(v)
    }

    fun commit() {
        if (inputText.isEmpty()) {
            setValue(null)
            return
        }
        val p = parsed()
        if (p == null) {
            inputText = value?.let { formatNumber(it) } ?: ""
            return
        }
        val snapped = if (step == null) clamp(p, minValue, maxValue) else snapValueToStep(p, minValue, maxValue, step)
        setValue(parseNumber(formatNumber(snapped)) ?: snapped)
    }

    fun increment() = setValue(nextStep(parsed(), true, minValue, maxValue, clampStep))
    fun decrement() = setValue(nextStep(parsed(), false, minValue, maxValue, clampStep))
    val canIncrement = active && canStep(parsed(), true, minValue, maxValue, clampStep)
    val canDecrement = active && canStep(parsed(), false, minValue, maxValue, clampStep)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = textStyle)
        Row(
            modifier = groupModifier.semantics {
                if (!enabled) disabled()
                if (invalid) error(errorMessage ?: "Invalid")
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StepButton(
                modifier = decrementModifier,
                name = "Decrease $label",
                enabled = canDecrement,
                onPress = {
                    decrement()
                    inputFocus.requestFocus()
                },
                content = decrementButton,
            )
            BasicTextField(
                value = inputText,
                onValueChange = { if (isValidPartialNumber(it, minValue, maxValue)) inputText = it },
                modifier = modifier
                    .focusRequester(inputFocus)
                    .onFocusChanged {
                        if (hadFocus && !it.isFocused) commit()
                        hadFocus = it.isFocused
                    }
                    .onPreviewKeyEvent { event ->
                        if (!active || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionUp, Key.PageUp -> increment()
                            Key.DirectionDown, Key.PageDown -> decrement()
                            Key.MoveHome -> minValue?.let { setValue(it) }
                            Key.MoveEnd -> maxValue?.let { setValue(snapValueToStep(it, minValue, maxValue, clampStep)) }
                            Key.Enter, Key.NumPadEnter -> commit()
                            else -> return@onPreviewKeyEvent false
                        }
                        true
                    }
                    .semantics {
                        contentDescription = label
                        if (invalid) error(errorMessage ?: "Invalid")
                    },
                enabled = enabled,
                readOnly = readOnly,
                textStyle = textStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                interactionSource = interactionSource,
            )
            StepButton(
                modifier = incrementModifier,
                name = "Increase $label",
                enabled = canIncrement,
                onPress = {
                    increment()
                    inputFocus.requestFocus()
                },
                content = incrementButton,
            )
        }
        description?.let { BasicText(it, style = textStyle) }
        if (invalid && errorMessage != null) BasicText(errorMessage, style = textStyle)
    }
}

/** A step button: named, unfocusable (the reference's `excludeFromTabOrder`), disabled at a bound. */
@Composable
private fun StepButton(
    modifier: Modifier,
    name: String,
    enabled: Boolean,
    onPress: () -> Unit,
    content: @Composable () -> Unit,
) {
    AriaButton(
        onPress = onPress,
        enabled = enabled,
        modifier = modifier
            .focusProperties { canFocus = false }
            .semantics { contentDescription = name },
    ) {
        content()
    }
}
