package dev.oxzo.aria

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import dev.oxzo.aria.interactions.ariaCheckable

/**
 * Port of react-aria-components `Checkbox`: a labelled toggle with `checkbox` role, Space
 * toggles, Enter does not, pointer press toggles. `isIndeterminate` is presentational only
 * (the reference's words): it shows as the mixed state until the caller clears it, and a
 * toggle still flips `isSelected`. The accessible name is the content, as the reference's
 * label wraps the input.
 *
 * Reference contract: https://react-aria.adobe.com/Checkbox
 */
@Composable
fun AriaCheckbox(
    isSelected: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isIndeterminate: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val state = when {
        isIndeterminate -> ToggleableState.Indeterminate
        isSelected -> ToggleableState.On
        else -> ToggleableState.Off
    }
    Box(
        modifier = modifier.ariaCheckable(
            state = state,
            onToggle = { onChange(!isSelected) },
            enabled = enabled,
            readOnly = readOnly,
            invalid = invalid,
            role = Role.Checkbox,
            interactionSource = interactionSource,
        ),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}
