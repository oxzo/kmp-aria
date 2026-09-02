package dev.oxzo.aria.interactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Test-only switches. Nothing here ships in the library by default.
 */
object AriaDebug {
    /**
     * When true, [focusMarker] appends "(focused)" to the accessible name of a focused node.
     * Exists because `document.activeElement` is always the canvas on Compose for Web
     * (CMP-10679), so a browser-side test cannot observe focus any other way. The demo app
     * turns it on; the library leaves it off.
     */
    var focusMarker: Boolean = false
}

/**
 * Place BEFORE the focusable modifier in the chain (`Modifier.focusMarker("Bold").ariaPressable(…)`).
 * `contentDescription` crosses the web accessibility mirror as `aria-label`, which is why it
 * is the carrier: it is one of the eleven properties the framework reads.
 */
@Composable
fun Modifier.focusMarker(name: String): Modifier {
    if (!AriaDebug.focusMarker) return this
    var focused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { focused = it.isFocused }
        .semantics { if (focused) contentDescription = "$name (focused)" }
}
