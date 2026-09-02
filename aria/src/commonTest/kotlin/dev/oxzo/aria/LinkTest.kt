package dev.oxzo.aria

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Semantics-tree instrument for Link. The react-aria contract under test: focusable, press
 * activates, Enter activates and Space does not, disabled ignores all input, an href is opened
 * through the platform URI handler.
 */
@OptIn(ExperimentalTestApi::class)
class LinkTest {
    @Test
    fun pointerPressActivates() = runComposeUiTest {
        setContent { Counter() }
        onNodeWithTag("lnk").assertIsEnabled()
        onNodeWithTag("lnk").performClick()
        onNodeWithTag("count").assertTextEquals("1")
    }

    @Test
    fun enterActivates() = runComposeUiTest {
        setContent { Counter() }
        onNodeWithTag("lnk").requestFocus()
        onNodeWithTag("lnk").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("count").assertTextEquals("1")
    }

    @Test
    fun spaceDoesNotActivate() = runComposeUiTest {
        setContent { Counter() }
        onNodeWithTag("lnk").requestFocus()
        onNodeWithTag("lnk").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("count").assertTextEquals("0")
    }

    @Test
    fun isFocusable() = runComposeUiTest {
        setContent { Counter() }
        onNodeWithTag("lnk").requestFocus()
        onNodeWithTag("lnk").assertIsFocused()
    }

    @Test
    fun disabledIgnoresInput() = runComposeUiTest {
        setContent { Counter(enabled = false) }
        onNodeWithTag("lnk").assertIsNotEnabled()
        onNodeWithTag("lnk").performClick()
        onNodeWithTag("count").assertTextEquals("0")
    }

    @Test
    fun hrefOpensThroughUriHandler() = runComposeUiTest {
        var opened: String? = null
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened = uri
            }
        }
        setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                AriaLink(href = "https://example.test/docs", modifier = Modifier.testTag("lnk")) {
                    BasicText("Docs")
                }
            }
        }
        onNodeWithTag("lnk").performClick()
        assertEquals("https://example.test/docs", opened)
    }
}

@Composable
private fun Counter(enabled: Boolean = true) {
    var count by remember { mutableStateOf(0) }
    AriaLink(onPress = { count++ }, modifier = Modifier.testTag("lnk"), enabled = enabled) {
        BasicText("Follow me")
    }
    BasicText(text = "$count", modifier = Modifier.testTag("count"))
}
