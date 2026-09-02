package dev.oxzo.aria

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Port of react-aria-components `SearchField` (`Label` + `Input` + a clear `Button`). Contract
 * (`useSearchField`, `useSearchFieldState`): an `<input type="search">`, so `searchbox` named by
 * the label; Enter calls [onSubmit] with the value and leaves the value alone; Escape clears a
 * non-empty value and calls [onClear], and does nothing on an empty one (`useKeyboard`
 * shortcuts, off when disabled or read-only); the clear button is named "Clear search", is
 * excluded from the tab order, clears and calls [onClear], and keeps focus on the input. The
 * reference's starter hides the clear button while the field is empty; here it is not composed
 * then.
 *
 * Compose vocabulary: there is no search role, so the field is a text field with
 * `ImeAction.Search`, named through `contentDescription` as [AriaTextField] is. Compose has no
 * focusable-but-untabbable state, so the clear button is made unfocusable (`canFocus = false`),
 * which also keeps a pointer press from taking focus off the input.
 *
 * Reference contract: https://react-aria.adobe.com/SearchField
 */
@Composable
fun AriaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
    onSubmit: ((String) -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    description: String? = null,
    errorMessage: String? = null,
    textStyle: TextStyle = TextStyle.Default,
    interactionSource: MutableInteractionSource? = null,
    clearButtonModifier: Modifier = Modifier,
    clearButton: @Composable () -> Unit = { BasicText("✕", style = textStyle) },
) {
    val shortcuts = enabled && !readOnly
    val clear: () -> Unit = {
        onValueChange("")
        onClear?.invoke()
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = textStyle)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier
                    .onPreviewKeyEvent { event ->
                        if (!shortcuts || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Enter, Key.NumPadEnter -> if (onSubmit != null) {
                                onSubmit(value)
                                true
                            } else {
                                false
                            }
                            Key.Escape -> if (value.isNotEmpty()) {
                                clear()
                                true
                            } else {
                                false
                            }
                            else -> false
                        }
                    }
                    .semantics {
                        contentDescription = label
                        if (invalid) error(errorMessage ?: "Invalid")
                    },
                enabled = enabled,
                readOnly = readOnly,
                textStyle = textStyle,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                singleLine = true,
                interactionSource = interactionSource,
            )
            if (value.isNotEmpty()) {
                AriaButton(
                    onPress = clear,
                    enabled = shortcuts,
                    modifier = clearButtonModifier
                        .focusProperties { canFocus = false }
                        .semantics { contentDescription = "Clear search" },
                ) {
                    clearButton()
                }
            }
        }
        description?.let { BasicText(it, style = textStyle) }
        if (invalid && errorMessage != null) BasicText(errorMessage, style = textStyle)
    }
}
