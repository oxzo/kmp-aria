package dev.oxzo.aria

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.oxzo.aria.interactions.ariaToggleable

/**
 * Port of react-aria-components `ToggleButton`: a button with a pressed state. The reference
 * exposes `aria-pressed`; this sets `toggleableState` on the semantics node, which is the
 * Compose vocabulary for the same thing.
 *
 * Reference contract: https://react-aria.adobe.com/ToggleButton
 */
@Composable
fun AriaToggleButton(
    isSelected: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.ariaToggleable(
            isSelected = isSelected,
            onChange = onChange,
            enabled = enabled,
            interactionSource = interactionSource,
        ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
