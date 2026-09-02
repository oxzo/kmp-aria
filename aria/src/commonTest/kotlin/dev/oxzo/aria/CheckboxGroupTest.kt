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
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Semantics-tree instrument for CheckboxGroup. The react-aria contract under test: click and
 * Space toggle a checkbox's membership of the group value, Enter does not; values keep press
 * order; a disabled or read-only group blocks every checkbox and a disabled checkbox stays off;
 * every enabled checkbox is its own tab stop; the group is named by its label and carries the
 * error, and its checkboxes are checkboxes.
 */
@OptIn(ExperimentalTestApi::class)
class CheckboxGroupTest {
    @Test
    fun clickAddsInPressOrder() = runComposeUiTest {
        setContent { Interests() }
        onNodeWithTag("music").performClick()
        onNodeWithTag("sports").performClick()
        onNodeWithTag("music").assertIsOn()
        onNodeWithTag("sports").assertIsOn()
        onNodeWithTag("value").assertTextEquals("music,sports")
    }

    @Test
    fun clickAgainRemoves() = runComposeUiTest {
        setContent { Interests() }
        onNodeWithTag("sports").performClick()
        onNodeWithTag("sports").performClick()
        onNodeWithTag("sports").assertIsOff()
        onNodeWithTag("value").assertTextEquals("none")
    }

    @Test
    fun spaceTogglesEnterDoesNot() = runComposeUiTest {
        setContent { Interests() }
        onNodeWithTag("sports").requestFocus()
        onNodeWithTag("sports").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("sports").assertIsOn()
        onNodeWithTag("sports").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("sports").assertIsOn()
    }

    @Test
    fun disabledCheckboxStaysOff() = runComposeUiTest {
        setContent { Interests() }
        onNodeWithTag("reading").assertIsNotEnabled()
        onNodeWithTag("reading").performClick()
        onNodeWithTag("reading").assertIsOff()
        onNodeWithTag("value").assertTextEquals("none")
    }

    @Test
    fun disabledGroupDisablesEveryCheckbox() = runComposeUiTest {
        setContent { Interests(enabled = false) }
        onNodeWithTag("sports").assertIsNotEnabled()
        onNodeWithTag("music").assertIsNotEnabled()
        onNodeWithTag("sports").performClick()
        onNodeWithTag("value").assertTextEquals("none")
    }

    @Test
    fun readOnlyGroupBlocksToggles() = runComposeUiTest {
        setContent { Interests(readOnly = true) }
        onNodeWithTag("sports").performClick()
        onNodeWithTag("sports").assertIsOff()
        onNodeWithTag("value").assertTextEquals("none")
    }

    @Test
    fun everyEnabledCheckboxIsATabStop() = runComposeUiTest {
        setContent { Interests(withNeighbours = true) }
        onNodeWithTag("before").requestFocus()
        onNodeWithTag("before").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("sports").assertIsFocused()
        onNodeWithTag("sports").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("music").assertIsFocused()
        onNodeWithTag("music").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("after").assertIsFocused()
    }

    @Test
    fun groupIsNamedByItsLabelAndItemsAreCheckboxes() = runComposeUiTest {
        setContent { Interests() }
        onNodeWithTag("group").assertContentDescriptionEquals("Interests")
        onNodeWithTag("sports").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
    }

    @Test
    fun invalidGroupCarriesTheErrorAndMarksItsCheckboxes() = runComposeUiTest {
        setContent { Interests(invalid = true) }
        onNodeWithTag("group").assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Pick at least one"))
        onNodeWithTag("sports").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }
}

@Composable
private fun Interests(
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
    withNeighbours: Boolean = false,
) {
    var value by remember { mutableStateOf(setOf<String>()) }
    Column {
        if (withNeighbours) AriaButton(onPress = {}, modifier = Modifier.testTag("before")) { BasicText("Before") }
        AriaCheckboxGroup(
            value = value,
            onChange = { value = it },
            label = "Interests",
            modifier = Modifier.testTag("group"),
            enabled = enabled,
            readOnly = readOnly,
            invalid = invalid,
            errorMessage = "Pick at least one",
        ) {
            AriaCheckbox("sports", modifier = Modifier.testTag("sports")) { BasicText("Sports") }
            AriaCheckbox("music", modifier = Modifier.testTag("music")) { BasicText("Music") }
            AriaCheckbox("reading", modifier = Modifier.testTag("reading"), enabled = false) { BasicText("Reading") }
        }
        if (withNeighbours) AriaButton(onPress = {}, modifier = Modifier.testTag("after")) { BasicText("After") }
        BasicText(value.joinToString(",").ifEmpty { "none" }, modifier = Modifier.testTag("value"))
    }
}
