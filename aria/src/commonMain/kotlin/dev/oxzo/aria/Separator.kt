package dev.oxzo.aria

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Port of react-aria-components `Separator`. Contract (`useSeparator`): an `<hr>` when
 * horizontal (implicit `separator` role, implicit horizontal orientation) and a
 * `<div role="separator" aria-orientation="vertical">` when vertical; no name, not focusable, no
 * keyboard, nothing interactive.
 *
 * Compose has no separator role and no orientation property, so the semantics tree carries
 * nothing a screen reader could read and the modifier chain adds no semantics of its own: the
 * port is a line of [thickness] across the parent's width (horizontal) or height (vertical).
 * What the tests can hold it to is that geometry and its inertness.
 *
 * Reference contract: https://react-aria.adobe.com/Separator
 */
@Composable
fun AriaSeparator(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    thickness: Dp = 1.dp,
    color: Color = Color.Black,
) {
    val extent = if (orientation == Orientation.Horizontal) {
        Modifier.fillMaxWidth().height(thickness)
    } else {
        Modifier.fillMaxHeight().width(thickness)
    }
    Box(modifier.then(extent).background(color))
}
