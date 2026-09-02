package dev.oxzo.aria

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Port of react-aria-components `TextField` (`Label` + `Input`, optional description and
 * error). Contract: `textbox` named by the label, value follows typing, disabled and
 * read-only states, `aria-invalid` with an error message, `type="password"` as [secure].
 *
 * Compose has no label association; the label text goes into `contentDescription`, which is
 * the property the web mirror writes as `aria-label`. `modifier` is applied to the input
 * node, so a caller's `testTag` targets what the reference's `<input>` corresponds to.
 *
 * Reference contract: https://react-aria.adobe.com/TextField
 */
@Composable
fun AriaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
    secure: Boolean = false,
    description: String? = null,
    errorMessage: String? = null,
    textStyle: TextStyle = TextStyle.Default,
    interactionSource: MutableInteractionSource? = null,
    decoration: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() },
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = textStyle)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.semantics {
                contentDescription = label
                if (invalid) error(errorMessage ?: "Invalid")
            },
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            keyboardOptions = if (secure) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
            singleLine = true,
            visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
            interactionSource = interactionSource,
            decorationBox = decoration,
        )
        description?.let { BasicText(it, style = textStyle) }
        if (invalid && errorMessage != null) BasicText(errorMessage, style = textStyle)
    }
}
