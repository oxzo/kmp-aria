package dev.oxzo.aria.demo

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.window
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Web accessibility mirror is on by default (isA11YEnabled). Left on deliberately.
    ComposeViewport("composeApp") {
        var route by remember { mutableStateOf(window.location.hash) }
        DisposableEffect(Unit) {
            val listener: (Event) -> Unit = { route = window.location.hash }
            window.addEventListener("hashchange", listener)
            onDispose { window.removeEventListener("hashchange", listener) }
        }
        App(route)
    }
}
