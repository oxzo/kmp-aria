package dev.oxzo.aria

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.oxzo.aria.stately.formatNumber
import kotlin.test.Test

/**
 * Semantics-tree instrument for NumberField. The react-aria contract under test: ArrowUp/Down and
 * PageUp/Down step, Home/End jump to the bounds, stepping stops at the bounds and disables the
 * button there; typing is filtered to partial numbers; Enter and focus loss commit, clamping and
 * snapping; an empty commit clears, and a step from empty starts at the minimum; the step buttons
 * are not tab stops and a press on one steps and focuses the input; names come from the label;
 * read-only turns shortcuts and buttons off; disabled is disabled.
 */
@OptIn(ExperimentalTestApi::class)
class NumberFieldTest {
    @Test
    fun arrowUpIncrements() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performKeyInput { pressKey(Key.DirectionUp) }
        onNodeWithTag("value").assertTextEquals("Value: 6")
        onNodeWithTag("nf").assert(hasText("6"))
    }

    @Test
    fun arrowDownDecrements() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithTag("value").assertTextEquals("Value: 4")
    }

    @Test
    fun pageKeysStep() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performKeyInput { pressKey(Key.PageUp) }
        onNodeWithTag("value").assertTextEquals("Value: 6")
        onNodeWithTag("nf").performKeyInput { pressKey(Key.PageDown) }
        onNodeWithTag("value").assertTextEquals("Value: 5")
    }

    @Test
    fun homeAndEndJumpToTheBounds() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performKeyInput { pressKey(Key.MoveHome) }
        onNodeWithTag("value").assertTextEquals("Value: 0")
        onNodeWithTag("nf").performKeyInput { pressKey(Key.MoveEnd) }
        onNodeWithTag("value").assertTextEquals("Value: 10")
    }

    @Test
    fun incrementStopsAtTheMaximumAndDisablesTheButton() = runComposeUiTest {
        setContent { Field(initial = 10.0) }
        onNodeWithTag("inc").assertIsNotEnabled()
        onNodeWithTag("dec").assertIsEnabled()
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performKeyInput { pressKey(Key.DirectionUp) }
        onNodeWithTag("value").assertTextEquals("Value: 10")
    }

    @Test
    fun decrementStopsAtTheMinimumAndDisablesTheButton() = runComposeUiTest {
        setContent { Field(initial = 0.0) }
        onNodeWithTag("dec").assertIsNotEnabled()
        onNodeWithTag("inc").assertIsEnabled()
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithTag("value").assertTextEquals("Value: 0")
    }

    @Test
    fun enterCommitsTypedValueClampedToTheRange() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performTextClearance()
        onNodeWithTag("nf").performTextInput("42")
        onNodeWithTag("value").assertTextEquals("Value: 5")
        onNodeWithTag("nf").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("value").assertTextEquals("Value: 10")
        onNodeWithTag("nf").assert(hasText("10"))
    }

    @Test
    fun lettersAreRejected() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performTextInput("abc")
        onNodeWithTag("nf").assert(hasText("5"))
    }

    @Test
    fun negativeIsTypedWhenTheMinimumAllowsIt() = runComposeUiTest {
        setContent { Field(min = -5.0) }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performTextClearance()
        onNodeWithTag("nf").performTextInput("-3")
        onNodeWithTag("nf").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("value").assertTextEquals("Value: -3")
    }

    @Test
    fun emptyCommitClearsAndAStepFromEmptyStartsAtTheMinimum() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performTextClearance()
        onNodeWithTag("nf").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("value").assertTextEquals("Value: none")
        onNodeWithTag("inc").assertIsEnabled()
        onNodeWithTag("nf").performKeyInput { pressKey(Key.DirectionUp) }
        onNodeWithTag("value").assertTextEquals("Value: 0")
    }

    @Test
    fun focusLossCommits() = runComposeUiTest {
        setContent { Field(withNeighbours = true) }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performTextClearance()
        onNodeWithTag("nf").performTextInput("3")
        onNodeWithTag("value").assertTextEquals("Value: 5")
        onNodeWithTag("after").requestFocus()
        onNodeWithTag("value").assertTextEquals("Value: 3")
    }

    @Test
    fun stepButtonsAreNotTabStops() = runComposeUiTest {
        setContent { Field(withNeighbours = true) }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("after").assertIsFocused()
    }

    @Test
    fun pressingIncrementStepsAndFocusesTheInput() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("inc").performClick()
        onNodeWithTag("value").assertTextEquals("Value: 6")
        onNodeWithTag("nf").assertIsFocused()
    }

    @Test
    fun namesComeFromTheLabel() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("nf").assertContentDescriptionEquals("Quantity")
        onNodeWithTag("inc").assertContentDescriptionEquals("Increase Quantity")
        onNodeWithTag("dec").assertContentDescriptionEquals("Decrease Quantity")
    }

    @Test
    fun readOnlyIgnoresTheShortcutsAndDisablesTheButtons() = runComposeUiTest {
        setContent { Field(readOnly = true) }
        onNodeWithTag("inc").assertIsNotEnabled()
        onNodeWithTag("dec").assertIsNotEnabled()
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performKeyInput { pressKey(Key.DirectionUp) }
        onNodeWithTag("value").assertTextEquals("Value: 5")
    }

    @Test
    fun disabledIsNotEnabled() = runComposeUiTest {
        setContent { Field(enabled = false) }
        onNodeWithTag("nf").assertIsNotEnabled()
        onNodeWithTag("inc").assertIsNotEnabled()
    }

    @Test
    fun stepSnapsTypedValuesAndArrows() = runComposeUiTest {
        setContent { Field(initial = 1.0, step = 0.5) }
        onNodeWithTag("nf").requestFocus()
        onNodeWithTag("nf").performTextClearance()
        onNodeWithTag("nf").performTextInput("1.3")
        onNodeWithTag("nf").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("value").assertTextEquals("Value: 1.5")
        onNodeWithTag("nf").performKeyInput { pressKey(Key.DirectionUp) }
        onNodeWithTag("value").assertTextEquals("Value: 2")
    }
}

@Composable
private fun Field(
    initial: Double? = 5.0,
    min: Double? = 0.0,
    max: Double? = 10.0,
    step: Double? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    withNeighbours: Boolean = false,
) {
    var value by remember { mutableStateOf(initial) }
    Column {
        if (withNeighbours) AriaButton(onPress = {}, modifier = Modifier.testTag("before")) { BasicText("Before") }
        AriaNumberField(
            value = value,
            onValueChange = { value = it },
            label = "Quantity",
            modifier = Modifier.testTag("nf"),
            minValue = min,
            maxValue = max,
            step = step,
            enabled = enabled,
            readOnly = readOnly,
            groupModifier = Modifier.testTag("group"),
            incrementModifier = Modifier.testTag("inc"),
            decrementModifier = Modifier.testTag("dec"),
        )
        if (withNeighbours) AriaButton(onPress = {}, modifier = Modifier.testTag("after")) { BasicText("After") }
        BasicText("Value: ${value?.let { formatNumber(it) } ?: "none"}", modifier = Modifier.testTag("value"))
    }
}
