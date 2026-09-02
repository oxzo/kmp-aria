package dev.oxzo.aria

import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.oxzo.aria.stately.SelectionMode
import kotlin.test.Test

/**
 * Semantics-tree instrument for ToggleButtonGroup. The react-aria contract under test: single
 * selection replaces and re-pressing deselects (unless empty selection is disallowed); multiple
 * selection accumulates; Enter, Space and click all toggle; arrows along the orientation move
 * focus without wrapping, without selecting and skipping disabled items; cross-axis arrows do
 * nothing; the group is one tab stop that restores the last focused item; the roles are radio in
 * a selectable group (single) or toggleable buttons (multiple).
 */
@OptIn(ExperimentalTestApi::class)
class ToggleButtonGroupTest {
    @Test
    fun singleClickSelectsOneAtATime() = runComposeUiTest {
        setContent { Groups() }
        onNodeWithTag("center").performClick()
        onNodeWithTag("center").assertIsSelected()
        onNodeWithTag("left").assertIsNotSelected()
        onNodeWithTag("right").performClick()
        onNodeWithTag("right").assertIsSelected()
        onNodeWithTag("center").assertIsNotSelected()
        onNodeWithTag("value").assertTextEquals("right")
    }

    @Test
    fun singleReclickDeselects() = runComposeUiTest {
        setContent { Groups() }
        onNodeWithTag("center").performClick()
        onNodeWithTag("center").performClick()
        onNodeWithTag("center").assertIsNotSelected()
        onNodeWithTag("value").assertTextEquals("none")
    }

    @Test
    fun singleDisallowEmptyKeepsTheSelection() = runComposeUiTest {
        setContent { Groups(disallowEmpty = true) }
        onNodeWithTag("center").performClick()
        onNodeWithTag("center").performClick()
        onNodeWithTag("center").assertIsSelected()
    }

    @Test
    fun spaceAndEnterBothToggle() = runComposeUiTest {
        setContent { Groups() }
        onNodeWithTag("left").requestFocus()
        onNodeWithTag("left").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("left").assertIsSelected()
        onNodeWithTag("left").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("left").assertIsNotSelected()
    }

    @Test
    fun arrowRightMovesFocusWithoutSelecting() = runComposeUiTest {
        setContent { Groups() }
        onNodeWithTag("left").requestFocus()
        onNodeWithTag("left").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("left").performKeyInput { pressKey(Key.DirectionRight) }
        onNodeWithTag("center").assertIsFocused()
        onNodeWithTag("center").assertIsNotSelected()
        onNodeWithTag("left").assertIsSelected()
    }

    @Test
    fun arrowsDoNotWrap() = runComposeUiTest {
        setContent { Groups() }
        onNodeWithTag("left").requestFocus()
        onNodeWithTag("left").performKeyInput { pressKey(Key.DirectionLeft) }
        onNodeWithTag("left").assertIsFocused()
        onNodeWithTag("right").performClick()
        onNodeWithTag("right").assertIsFocused()
        onNodeWithTag("right").performKeyInput { pressKey(Key.DirectionRight) }
        onNodeWithTag("right").assertIsFocused()
    }

    @Test
    fun arrowsSkipDisabledItems() = runComposeUiTest {
        setContent { Groups(centerEnabled = false) }
        onNodeWithTag("left").requestFocus()
        onNodeWithTag("left").performKeyInput { pressKey(Key.DirectionRight) }
        onNodeWithTag("right").assertIsFocused()
    }

    @Test
    fun verticalUsesDownAndUpOnly() = runComposeUiTest {
        setContent { Groups(orientation = Orientation.Vertical) }
        onNodeWithTag("left").requestFocus()
        onNodeWithTag("left").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithTag("center").assertIsFocused()
        onNodeWithTag("center").performKeyInput { pressKey(Key.DirectionRight) }
        onNodeWithTag("center").assertIsFocused()
        onNodeWithTag("center").performKeyInput { pressKey(Key.DirectionUp) }
        onNodeWithTag("left").assertIsFocused()
    }

    @Test
    fun horizontalIgnoresDownAndUp() = runComposeUiTest {
        setContent { Groups() }
        onNodeWithTag("left").requestFocus()
        onNodeWithTag("left").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithTag("left").assertIsFocused()
    }

    @Test
    fun multipleAccumulatesAndTogglesOff() = runComposeUiTest {
        setContent { Groups(mode = SelectionMode.Multiple) }
        onNodeWithTag("left").performClick()
        onNodeWithTag("center").performClick()
        onNodeWithTag("left").assertIsOn()
        onNodeWithTag("center").assertIsOn()
        onNodeWithTag("value").assertTextEquals("left,center")
        onNodeWithTag("left").performClick()
        onNodeWithTag("left").assertIsOff()
        onNodeWithTag("center").assertIsOn()
    }

    @Test
    fun multipleDisallowEmptyKeepsTheLastItem() = runComposeUiTest {
        setContent { Groups(mode = SelectionMode.Multiple, disallowEmpty = true) }
        onNodeWithTag("left").performClick()
        onNodeWithTag("left").performClick()
        onNodeWithTag("left").assertIsOn()
    }

    @Test
    fun groupIsOneTabStopThatRestoresTheLastFocusedItem() = runComposeUiTest {
        setContent { Groups(withNeighbours = true) }
        onNodeWithTag("before").requestFocus()
        onNodeWithTag("before").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("left").assertIsFocused()
        onNodeWithTag("left").performKeyInput { pressKey(Key.DirectionRight) }
        onNodeWithTag("center").assertIsFocused()
        onNodeWithTag("center").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("after").assertIsFocused()
        onNodeWithTag("before").requestFocus()
        onNodeWithTag("before").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("center").assertIsFocused()
    }

    @Test
    fun singleSemanticsAreRadiosInASelectableGroup() = runComposeUiTest {
        setContent { Groups() }
        onNodeWithTag("left").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        onNodeWithTag("group").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        onNodeWithTag("group").assertContentDescriptionEquals("Text alignment")
    }

    @Test
    fun multipleSemanticsAreToggleableButtons() = runComposeUiTest {
        setContent { Groups(mode = SelectionMode.Multiple) }
        onNodeWithTag("left").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        onNodeWithTag("left").assertIsOff()
        onNodeWithTag("group").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.SelectableGroup))
    }

    @Test
    fun disabledGroupIgnoresInput() = runComposeUiTest {
        setContent { Groups(enabled = false) }
        onNodeWithTag("left").assertIsNotEnabled()
        onNodeWithTag("left").performClick()
        onNodeWithTag("value").assertTextEquals("none")
    }
}

@Composable
private fun Groups(
    mode: SelectionMode = SelectionMode.Single,
    disallowEmpty: Boolean = false,
    orientation: Orientation = Orientation.Horizontal,
    centerEnabled: Boolean = true,
    enabled: Boolean = true,
    withNeighbours: Boolean = false,
) {
    var keys by remember { mutableStateOf(setOf<String>()) }
    Column {
        if (withNeighbours) AriaButton(onPress = {}, modifier = Modifier.testTag("before")) { BasicText("Before") }
        AriaToggleButtonGroup(
            selectedKeys = keys,
            onSelectionChange = { keys = it },
            modifier = Modifier.testTag("group"),
            selectionMode = mode,
            disallowEmptySelection = disallowEmpty,
            orientation = orientation,
            enabled = enabled,
            label = "Text alignment",
        ) {
            AriaToggleButton("left", modifier = Modifier.testTag("left")) { BasicText("Left") }
            AriaToggleButton("center", modifier = Modifier.testTag("center"), enabled = centerEnabled) { BasicText("Center") }
            AriaToggleButton("right", modifier = Modifier.testTag("right")) { BasicText("Right") }
        }
        if (withNeighbours) AriaButton(onPress = {}, modifier = Modifier.testTag("after")) { BasicText("After") }
        BasicText(keys.joinToString(",").ifEmpty { "none" }, modifier = Modifier.testTag("value"))
    }
}
