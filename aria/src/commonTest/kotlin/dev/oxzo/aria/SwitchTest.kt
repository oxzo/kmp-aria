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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
 * Semantics-tree instrument for Switch: the Checkbox contract with the `switch` role and no
 * mixed state.
 */
@OptIn(ExperimentalTestApi::class)
class SwitchTest {
    @Test
    fun pointerPressToggles() = runComposeUiTest {
        setContent { Sw() }
        onNodeWithTag("sw").assertIsOff()
        onNodeWithTag("sw").performClick()
        onNodeWithTag("sw").assertIsOn()
    }

    @Test
    fun spaceToggles() = runComposeUiTest {
        setContent { Sw() }
        onNodeWithTag("sw").requestFocus()
        onNodeWithTag("sw").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("sw").assertIsOn()
    }

    @Test
    fun enterDoesNotToggle() = runComposeUiTest {
        setContent { Sw() }
        onNodeWithTag("sw").requestFocus()
        onNodeWithTag("sw").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("sw").assertIsOff()
    }

    @Test
    fun roleIsSwitch() = runComposeUiTest {
        setContent { Sw() }
        onNodeWithTag("sw").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
    }

    @Test
    fun disabledKeepsState() = runComposeUiTest {
        setContent { Sw(initial = true, enabled = false) }
        onNodeWithTag("sw").assertIsNotEnabled()
        onNodeWithTag("sw").performClick()
        onNodeWithTag("sw").assertIsOn()
    }
}

@Composable
private fun Sw(initial: Boolean = false, enabled: Boolean = true) {
    var on by remember { mutableStateOf(initial) }
    AriaSwitch(
        isSelected = on,
        onChange = { on = it },
        modifier = Modifier.testTag("sw"),
        enabled = enabled,
    ) {
        BasicText("Wi-Fi")
    }
}
