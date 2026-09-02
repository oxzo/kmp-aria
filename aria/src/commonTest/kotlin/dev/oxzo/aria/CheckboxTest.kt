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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
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
 * Semantics-tree instrument for Checkbox. The react-aria contract under test: pointer press
 * toggles, Space toggles, Enter does not (native checkbox input), `checkbox` role, mixed state
 * shown until the caller clears it, disabled ignores input, read-only is focusable but inert,
 * invalid is exposed.
 */
@OptIn(ExperimentalTestApi::class)
class CheckboxTest {
    @Test
    fun pointerPressToggles() = runComposeUiTest {
        setContent { Check() }
        onNodeWithTag("cb").assertIsOff()
        onNodeWithTag("cb").performClick()
        onNodeWithTag("cb").assertIsOn()
        onNodeWithTag("cb").performClick()
        onNodeWithTag("cb").assertIsOff()
    }

    @Test
    fun spaceToggles() = runComposeUiTest {
        setContent { Check() }
        onNodeWithTag("cb").requestFocus()
        onNodeWithTag("cb").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("cb").assertIsOn()
    }

    @Test
    fun enterDoesNotToggle() = runComposeUiTest {
        setContent { Check() }
        onNodeWithTag("cb").requestFocus()
        onNodeWithTag("cb").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("cb").assertIsOff()
    }

    @Test
    fun roleIsCheckbox() = runComposeUiTest {
        setContent { Check() }
        onNodeWithTag("cb").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
    }

    @Test
    fun indeterminateShowsUntilCleared() = runComposeUiTest {
        setContent { Check(indeterminate = true) }
        onNodeWithTag("cb").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate),
        )
        onNodeWithTag("cb").performClick()
        onNodeWithTag("cb").assertIsOn()
    }

    @Test
    fun disabledKeepsState() = runComposeUiTest {
        setContent { Check(initial = true, enabled = false) }
        onNodeWithTag("cb").assertIsNotEnabled()
        onNodeWithTag("cb").performClick()
        onNodeWithTag("cb").assertIsOn()
    }

    @Test
    fun readOnlyIsFocusableButInert() = runComposeUiTest {
        setContent { Check(readOnly = true) }
        onNodeWithTag("cb").requestFocus()
        onNodeWithTag("cb").assertIsFocused()
        onNodeWithTag("cb").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("cb").performClick()
        onNodeWithTag("cb").assertIsOff()
    }

    @Test
    fun invalidExposesError() = runComposeUiTest {
        setContent { Check(invalid = true) }
        onNodeWithTag("cb").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }
}

@Composable
private fun Check(
    initial: Boolean = false,
    indeterminate: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
) {
    var selected by remember { mutableStateOf(initial) }
    var mixed by remember { mutableStateOf(indeterminate) }
    AriaCheckbox(
        isSelected = selected,
        onChange = { selected = it; mixed = false },
        modifier = Modifier.testTag("cb"),
        isIndeterminate = mixed,
        enabled = enabled,
        readOnly = readOnly,
        invalid = invalid,
    ) {
        BasicText("Subscribe")
    }
}
