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
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Semantics-tree instrument for SearchField. The react-aria contract under test: typing updates
 * the value; Enter submits the value and keeps it; Escape clears a non-empty value and calls
 * onClear, and does nothing when empty; the clear button exists only with a value, clears, is
 * not a tab stop and leaves focus on the field; read-only disables the shortcuts; the field is
 * named by its label and the clear button "Clear search".
 */
@OptIn(ExperimentalTestApi::class)
class SearchFieldTest {
    @Test
    fun typingUpdatesValue() = runComposeUiTest {
        setContent { Search() }
        onNodeWithTag("sf").requestFocus()
        onNodeWithTag("sf").performTextInput("kotlin")
        onNodeWithTag("value").assertTextEquals("Value: kotlin")
    }

    @Test
    fun enterSubmitsAndKeepsTheValue() = runComposeUiTest {
        setContent { Search() }
        onNodeWithTag("sf").requestFocus()
        onNodeWithTag("sf").performTextInput("kotlin")
        onNodeWithTag("sf").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("submitted").assertTextEquals("Submitted: kotlin")
        onNodeWithTag("value").assertTextEquals("Value: kotlin")
    }

    @Test
    fun escapeClearsAndCallsOnClear() = runComposeUiTest {
        setContent { Search() }
        onNodeWithTag("sf").requestFocus()
        onNodeWithTag("sf").performTextInput("kotlin")
        onNodeWithTag("sf").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("value").assertTextEquals("Value: ")
        onNodeWithTag("cleared").assertTextEquals("Cleared: 1")
    }

    @Test
    fun escapeOnAnEmptyFieldDoesNothing() = runComposeUiTest {
        setContent { Search() }
        onNodeWithTag("sf").requestFocus()
        onNodeWithTag("sf").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("cleared").assertTextEquals("Cleared: 0")
    }

    @Test
    fun clearButtonExistsOnlyWithAValue() = runComposeUiTest {
        setContent { Search() }
        onNodeWithTag("clear").assertDoesNotExist()
        onNodeWithTag("sf").requestFocus()
        onNodeWithTag("sf").performTextInput("k")
        onNodeWithTag("clear").assertExists()
        onNodeWithTag("clear").assertContentDescriptionEquals("Clear search")
    }

    @Test
    fun clearButtonClearsAndLeavesFocusOnTheField() = runComposeUiTest {
        setContent { Search() }
        onNodeWithTag("sf").requestFocus()
        onNodeWithTag("sf").performTextInput("kotlin")
        onNodeWithTag("clear").performClick()
        onNodeWithTag("value").assertTextEquals("Value: ")
        onNodeWithTag("cleared").assertTextEquals("Cleared: 1")
        onNodeWithTag("sf").assertIsFocused()
    }

    @Test
    fun clearButtonIsNotATabStop() = runComposeUiTest {
        setContent { Search(initial = "kotlin", withNeighbours = true) }
        onNodeWithTag("sf").requestFocus()
        onNodeWithTag("sf").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("after").assertIsFocused()
    }

    @Test
    fun labelIsTheAccessibleName() = runComposeUiTest {
        setContent { Search() }
        onNodeWithTag("sf").assertContentDescriptionEquals("Search")
    }

    @Test
    fun readOnlyIgnoresTheShortcuts() = runComposeUiTest {
        setContent { Search(initial = "kotlin", readOnly = true) }
        onNodeWithTag("sf").requestFocus()
        onNodeWithTag("sf").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("value").assertTextEquals("Value: kotlin")
        onNodeWithTag("sf").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("submitted").assertTextEquals("Submitted: ")
    }

    @Test
    fun disabledIsNotEnabled() = runComposeUiTest {
        setContent { Search(enabled = false) }
        onNodeWithTag("sf").assertIsNotEnabled()
    }
}

@Composable
private fun Search(
    initial: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    withNeighbours: Boolean = false,
) {
    var value by remember { mutableStateOf(initial) }
    var submitted by remember { mutableStateOf("") }
    var cleared by remember { mutableStateOf(0) }
    Column {
        if (withNeighbours) AriaButton(onPress = {}, modifier = Modifier.testTag("before")) { BasicText("Before") }
        AriaSearchField(
            value = value,
            onValueChange = { value = it },
            label = "Search",
            modifier = Modifier.testTag("sf"),
            enabled = enabled,
            readOnly = readOnly,
            onSubmit = { submitted = it },
            onClear = { cleared++ },
            clearButtonModifier = Modifier.testTag("clear"),
        )
        if (withNeighbours) AriaButton(onPress = {}, modifier = Modifier.testTag("after")) { BasicText("After") }
        BasicText("Value: $value", modifier = Modifier.testTag("value"))
        BasicText("Submitted: $submitted", modifier = Modifier.testTag("submitted"))
        BasicText("Cleared: $cleared", modifier = Modifier.testTag("cleared"))
    }
}
