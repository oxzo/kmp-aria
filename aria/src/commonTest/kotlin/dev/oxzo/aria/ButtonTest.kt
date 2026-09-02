package dev.oxzo.aria

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Semantics-tree instrument for Button. The react-aria contract under test: press activates,
 * Enter and Space activate from the keyboard, disabled ignores all input.
 */
@OptIn(ExperimentalTestApi::class)
class ButtonTest {
    @Test
    fun pointerPressActivates() = runComposeUiTest {
        setContent { Counter() }
        onNodeWithTag("btn").assertIsEnabled()
        onNodeWithTag("btn").performClick()
        onNodeWithTag("count").assertTextEquals("1")
    }

    @Test
    fun enterAndSpaceActivate() = runComposeUiTest {
        setContent { Counter() }
        onNodeWithTag("btn").requestFocus()
        onNodeWithTag("btn").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("count").assertTextEquals("1")
        onNodeWithTag("btn").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("count").assertTextEquals("2")
    }

    @Test
    fun disabledIgnoresInput() = runComposeUiTest {
        setContent { Counter(enabled = false) }
        onNodeWithTag("btn").assertIsNotEnabled()
        onNodeWithTag("btn").performClick()
        onNodeWithTag("count").assertTextEquals("0")
    }
}

@androidx.compose.runtime.Composable
private fun Counter(enabled: Boolean = true) {
    var count by remember { mutableStateOf(0) }
    AriaButton(onPress = { count++ }, modifier = Modifier.testTag("btn"), enabled = enabled) {
        BasicText("Press me")
    }
    BasicText(text = "$count", modifier = Modifier.testTag("count"))
}
