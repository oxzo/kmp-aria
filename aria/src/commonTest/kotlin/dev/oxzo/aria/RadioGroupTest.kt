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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Semantics-tree instrument for RadioGroup. The react-aria contract under test: click and
 * Space select, Enter does not, arrows move focus and selection with wrap-around and skip
 * disabled radios, read-only moves focus without selecting, the group is one tab stop.
 */
@OptIn(ExperimentalTestApi::class)
class RadioGroupTest {
    @Test
    fun clickSelects() = runComposeUiTest {
        setContent { Pets() }
        onNodeWithTag("cat").performClick()
        onNodeWithTag("cat").assertIsSelected()
        onNodeWithTag("dog").assertIsNotSelected()
        onNodeWithTag("value").assertTextEquals("cat")
    }

    @Test
    fun spaceSelectsFocused() = runComposeUiTest {
        setContent { Pets() }
        onNodeWithTag("dog").requestFocus()
        onNodeWithTag("dog").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("dog").assertIsSelected()
    }

    @Test
    fun enterDoesNotSelect() = runComposeUiTest {
        setContent { Pets() }
        onNodeWithTag("dog").requestFocus()
        onNodeWithTag("dog").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("dog").assertIsNotSelected()
    }

    @Test
    fun arrowDownMovesFocusAndSelects() = runComposeUiTest {
        setContent { Pets() }
        onNodeWithTag("dog").requestFocus()
        onNodeWithTag("dog").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithTag("cat").assertIsFocused()
        onNodeWithTag("cat").assertIsSelected()
    }

    @Test
    fun arrowUpWrapsToLast() = runComposeUiTest {
        setContent { Pets() }
        onNodeWithTag("dog").requestFocus()
        onNodeWithTag("dog").performKeyInput { pressKey(Key.DirectionUp) }
        onNodeWithTag("dragon").assertIsFocused()
        onNodeWithTag("dragon").assertIsSelected()
    }

    @Test
    fun arrowsSkipDisabledRadios() = runComposeUiTest {
        setContent { Pets(catEnabled = false) }
        onNodeWithTag("dog").requestFocus()
        onNodeWithTag("dog").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithTag("dragon").assertIsFocused()
        onNodeWithTag("dragon").assertIsSelected()
    }

    @Test
    fun readOnlyMovesFocusWithoutSelecting() = runComposeUiTest {
        setContent { Pets(readOnly = true) }
        onNodeWithTag("dog").requestFocus()
        onNodeWithTag("dog").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithTag("cat").assertIsFocused()
        onNodeWithTag("cat").assertIsNotSelected()
        onNodeWithTag("value").assertTextEquals("none")
    }

    @Test
    fun groupIsOneTabStopAtTheSelectedRadio() = runComposeUiTest {
        setContent { Pets(initial = "cat", withNeighbours = true) }
        onNodeWithTag("before").requestFocus()
        onNodeWithTag("before").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("cat").assertIsFocused()
        onNodeWithTag("cat").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("after").assertIsFocused()
    }

    @Test
    fun rolesAreRadioInsideASelectableGroup() = runComposeUiTest {
        setContent { Pets() }
        onNodeWithTag("dog").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        onNodeWithTag("group").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }
}

@Composable
private fun Pets(
    initial: String? = null,
    catEnabled: Boolean = true,
    readOnly: Boolean = false,
    withNeighbours: Boolean = false,
) {
    var value by remember { mutableStateOf(initial) }
    Column {
        if (withNeighbours) AriaButton(onPress = {}, modifier = Modifier.testTag("before")) { BasicText("Before") }
        AriaRadioGroup(
            value = value,
            onChange = { value = it },
            label = "Favorite pet",
            modifier = Modifier.testTag("group"),
            readOnly = readOnly,
        ) {
            AriaRadio("dog", modifier = Modifier.testTag("dog")) { BasicText("Dog") }
            AriaRadio("cat", modifier = Modifier.testTag("cat"), enabled = catEnabled) { BasicText("Cat") }
            AriaRadio("dragon", modifier = Modifier.testTag("dragon")) { BasicText("Dragon") }
        }
        if (withNeighbours) AriaButton(onPress = {}, modifier = Modifier.testTag("after")) { BasicText("After") }
        BasicText(value ?: "none", modifier = Modifier.testTag("value"))
    }
}
