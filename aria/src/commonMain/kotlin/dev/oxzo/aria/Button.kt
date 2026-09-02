package dev.oxzo.aria

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.oxzo.aria.interactions.ariaPressable

/**
 * Port of react-aria-components `Button`: unstyled, focusable, activates on pointer press and
 * on Enter/Space; `enabled = false` removes it from the focus order and marks it disabled.
 *
 * Reference contract: https://react-aria.adobe.com/Button (WAI-ARIA button pattern).
 */
@Composable
fun AriaButton(
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.ariaPressable(
            onPress = onPress,
            enabled = enabled,
            interactionSource = interactionSource,
        ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
