package dev.oxzo.aria

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

/**
 * Semantics-tree instrument for Separator. Compose has no separator role or orientation property,
 * so the contract the tests can hold the port to is the geometry the orientation implies (a line
 * of the given thickness across the parent) and inertness: not focusable, nothing to click.
 */
@OptIn(ExperimentalTestApi::class)
class SeparatorTest {
    @Test
    fun horizontalSpansTheParentWidthAtItsThickness() = runComposeUiTest {
        setContent {
            Box(Modifier.size(200.dp, 100.dp)) {
                AriaSeparator(Modifier.testTag("sep"), thickness = 2.dp)
            }
        }
        onNodeWithTag("sep").assertWidthIsEqualTo(200.dp).assertHeightIsEqualTo(2.dp)
    }

    @Test
    fun verticalSpansTheParentHeightAtItsThickness() = runComposeUiTest {
        setContent {
            Box(Modifier.size(200.dp, 100.dp)) {
                AriaSeparator(Modifier.testTag("sep"), orientation = Orientation.Vertical, thickness = 2.dp)
            }
        }
        onNodeWithTag("sep").assertHeightIsEqualTo(100.dp).assertWidthIsEqualTo(2.dp)
    }

    @Test
    fun isNotFocusable() = runComposeUiTest {
        setContent { AriaSeparator(Modifier.testTag("sep")) }
        onNodeWithTag("sep").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
    }

    @Test
    fun hasNoClickAction() = runComposeUiTest {
        setContent { AriaSeparator(Modifier.testTag("sep")) }
        onNodeWithTag("sep").assertHasNoClickAction()
    }
}
