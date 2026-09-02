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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Semantics-tree instrument for Disclosure. The react-aria contract under test: the trigger is a
 * button inside a heading; press, Enter and Space toggle the panel; the expanded state is
 * exposed (here as the expand/collapse action pair) and actionable; disabled ignores input.
 */
@OptIn(ExperimentalTestApi::class)
class DisclosureTest {
    @Test
    fun pointerPressToggles() = runComposeUiTest {
        setContent { Disc() }
        onNodeWithTag("panel").assertDoesNotExist()
        onNodeWithTag("trig").performClick()
        onNodeWithTag("panel").assertExists()
        onNodeWithTag("state").assertTextEquals("Expanded")
        onNodeWithTag("trig").performClick()
        onNodeWithTag("panel").assertDoesNotExist()
        onNodeWithTag("state").assertTextEquals("Collapsed")
    }

    @Test
    fun enterToggles() = runComposeUiTest {
        setContent { Disc() }
        onNodeWithTag("trig").requestFocus()
        onNodeWithTag("trig").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("panel").assertExists()
        onNodeWithTag("trig").performKeyInput { pressKey(Key.Enter) }
        onNodeWithTag("panel").assertDoesNotExist()
    }

    @Test
    fun spaceToggles() = runComposeUiTest {
        setContent { Disc() }
        onNodeWithTag("trig").requestFocus()
        onNodeWithTag("trig").performKeyInput { pressKey(Key.Spacebar) }
        onNodeWithTag("panel").assertExists()
    }

    @Test
    fun expandActionWhenCollapsed() = runComposeUiTest {
        setContent { Disc() }
        onNodeWithTag("trig").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
        onNodeWithTag("trig").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
        onNodeWithTag("trig").performSemanticsAction(SemanticsActions.Expand)
        onNodeWithTag("panel").assertExists()
    }

    @Test
    fun collapseActionWhenExpanded() = runComposeUiTest {
        setContent { Disc(initial = true) }
        onNodeWithTag("panel").assertExists()
        onNodeWithTag("trig").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
        onNodeWithTag("trig").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
        onNodeWithTag("trig").performSemanticsAction(SemanticsActions.Collapse)
        onNodeWithTag("panel").assertDoesNotExist()
    }

    @Test
    fun triggerSitsInAHeading() = runComposeUiTest {
        setContent { Disc() }
        onNodeWithTag("trig").onParent().assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun disabledIgnoresInput() = runComposeUiTest {
        setContent { Disc(enabled = false) }
        onNodeWithTag("trig").assertIsNotEnabled()
        onNodeWithTag("trig").performClick()
        onNodeWithTag("panel").assertDoesNotExist()
    }
}

@Composable
private fun Disc(initial: Boolean = false, enabled: Boolean = true) {
    var open by remember { mutableStateOf(initial) }
    AriaDisclosure(
        isExpanded = open,
        onExpandedChange = { open = it },
        triggerModifier = Modifier.testTag("trig"),
        panelModifier = Modifier.testTag("panel"),
        enabled = enabled,
        trigger = { BasicText("System Requirements") },
    ) {
        BasicText("Details about system requirements here.")
    }
    BasicText(if (open) "Expanded" else "Collapsed", modifier = Modifier.testTag("state"))
}
