package dev.oxzo.aria

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import dev.oxzo.aria.interactions.enterOnlyActivation

/**
 * Port of react-aria-components `Link`: focusable, activates on pointer press and on Enter only.
 * Space does nothing: `usePress` lets only Enter through for `role="link"` and for anchors
 * (`isValidKeyboardEvent`: "Links should only trigger with Enter key"), the native `<a>`
 * contract. `enabled = false` removes it from the focus order and marks it disabled. [href] is
 * opened through [LocalUriHandler] on activation, after [onPress]; without an href the link is
 * the reference's `<span role="link">`, a press handler with link semantics.
 *
 * Compose's `Role` has no link value, so the node carries no role of its own. On the web the
 * 1.12.0 mirror emits every clickable node as `button` regardless; `jb-main` maps a text
 * `LinkAnnotation` (its `LinkTestMarker`) to `role="link"` but still not a standalone clickable.
 * The demo's framework control route (`#/fw-link`) measures the annotation path.
 *
 * Reference contract: https://react-aria.adobe.com/Link
 */
@Composable
fun AriaLink(
    href: String? = null,
    onPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = modifier
            .enterOnlyActivation()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    onPress?.invoke()
                    if (href != null) uriHandler.openUri(href)
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}
