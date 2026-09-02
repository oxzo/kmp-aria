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
 * Port of react-aria-components `Switch`: the checkbox interaction with the `switch` role.
 * Same keyboard contract as [AriaCheckbox] (Space toggles, Enter does not); no mixed state.
 *
 * Reference contract: https://react-aria.adobe.com/Switch
 */
@Composable
fun AriaSwitch(
    isSelected: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.ariaCheckable(
            state = if (isSelected) ToggleableState.On else ToggleableState.Off,
            onToggle = { onChange(!isSelected) },
            enabled = enabled,
            readOnly = readOnly,
            role = Role.Switch,
            interactionSource = interactionSource,
        ),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}
