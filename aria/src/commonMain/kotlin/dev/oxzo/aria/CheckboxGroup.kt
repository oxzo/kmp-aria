package dev.oxzo.aria

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle

/** Receiver for the grouped [AriaCheckbox] overload inside an [AriaCheckboxGroup]. */
class AriaCheckboxGroupScope internal constructor(
    val selectedValues: Set<String>,
    internal val onToggle: (String, Boolean) -> Unit,
    internal val groupEnabled: Boolean,
    internal val groupReadOnly: Boolean,
    internal val groupInvalid: Boolean,
)

/**
 * Port of react-aria-components `CheckboxGroup`. Contract (`useCheckboxGroup`,
 * `useCheckboxGroupState`, `useCheckboxGroupItem`): the group is `role="group"` named by its
 * label (`aria-labelledby`), `aria-disabled` when disabled; each child is a `checkbox` whose
 * selection is its value's membership of the group value; selecting appends the value and
 * deselecting removes it, so the value keeps press order; disabled and read-only on the group
 * apply to every checkbox; an invalid group marks every checkbox invalid; every checkbox is its
 * own tab stop (no roving focus, unlike [AriaRadioGroup]); description and error message are
 * described-by.
 *
 * Compose vocabulary: there is no group role, so the label goes into the group's
 * `contentDescription` (the property the web mirror writes as `aria-label`), `disabled()` when
 * disabled, `error()` when invalid. [selectedValues] is an insertion-ordered set standing in
 * for the reference's `string[]`.
 *
 * Reference contract: https://react-aria.adobe.com/CheckboxGroup
 */
@Composable
fun AriaCheckboxGroup(
    value: Set<String>,
    onChange: (Set<String>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
    description: String? = null,
    errorMessage: String? = null,
    labelStyle: TextStyle = TextStyle.Default,
    content: @Composable AriaCheckboxGroupScope.() -> Unit,
) {
    val scope = AriaCheckboxGroupScope(
        selectedValues = value,
        onToggle = { v, selected -> onChange(if (selected) value + v else value - v) },
        groupEnabled = enabled,
        groupReadOnly = readOnly,
        groupInvalid = invalid,
    )
    val groupModifier = modifier.semantics {
        contentDescription = label
        if (!enabled) disabled()
        if (invalid) error(errorMessage ?: "Invalid")
    }
    val body: @Composable () -> Unit = {
        BasicText(label, style = labelStyle)
        scope.content()
        description?.let { BasicText(it, style = labelStyle) }
        if (invalid && errorMessage != null) BasicText(errorMessage, style = labelStyle)
    }
    if (orientation == Orientation.Vertical) {
        Column(modifier = groupModifier) { body() }
    } else {
        Row(modifier = groupModifier, verticalAlignment = Alignment.CenterVertically) { body() }
    }
}

/**
 * One checkbox of an [AriaCheckboxGroup], keyed by [value] (the reference's `value` prop). Its
 * selection, enabled, read-only and invalid states come from the group; `modifier` lands on the
 * checkable node as in [AriaCheckbox].
 */
@Composable
fun AriaCheckboxGroupScope.AriaCheckbox(
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isIndeterminate: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    AriaCheckbox(
        isSelected = value in selectedValues,
        onChange = { onToggle(value, it) },
        modifier = modifier,
        isIndeterminate = isIndeterminate,
        enabled = enabled && groupEnabled,
        readOnly = readOnly || groupReadOnly,
        invalid = groupInvalid,
        interactionSource = interactionSource,
        content = content,
    )
}
