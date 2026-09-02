package dev.oxzo.aria

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Semantics-tree instrument for ToggleButton. The react-aria contract under test: press
 * toggles `aria-pressed` (here: toggleableState), Space and Enter toggle, disabled ignores
 * input and keeps its state.
 */
@OptIn(ExperimentalTestApi::class)
class ToggleButtonTest {
    @Test
    fun pointerPressToggles() = runComposeUiTest {
        setContent { Toggle() }
        onNodeWithTag("tb").assertIsOff()
        onNodeWithTag("tb").performClick()
        onNodeWithTag("tb").assertIsOn()
        onNodeWithTag("tb").performClick()
        onNodeWithTag("tb").assertIsOff()
    }

    @Test
    fun spaceAndEnterToggle() = runComposeUiTest {
        setContent { Toggle() }
        onNodeWithTag("tb").requestFocus()
        onNodeWithTag("tb").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("tb").assertIsOn()
        onNodeWithTag("tb").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("tb").assertIsOff()
    }

    @Test
    fun disabledKeepsState() = runComposeUiTest {
        setContent { Toggle(initial = true, enabled = false) }
        onNodeWithTag("tb").assertIsNotEnabled()
        onNodeWithTag("tb").assertIsOn()
        onNodeWithTag("tb").performClick()
        onNodeWithTag("tb").assertIsOn()
    }
}

@Composable
private fun Toggle(initial: Boolean = false, enabled: Boolean = true) {
    var selected by remember { mutableStateOf(initial) }
    AriaToggleButton(
        isSelected = selected,
        onChange = { selected = it },
        modifier = Modifier.testTag("tb"),
        enabled = enabled,
    ) {
        BasicText("Bold")
    }
}
