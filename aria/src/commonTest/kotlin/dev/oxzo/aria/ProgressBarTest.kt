package dev.oxzo.aria

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Semantics-tree instrument for ProgressBar. The react-aria contract under test: the label is
 * the accessible name, the range info follows the value within its range, the value text is a
 * percent of the range, indeterminate carries no value.
 */
@OptIn(ExperimentalTestApi::class)
class ProgressBarTest {
    @Test
    fun rangeInfoFollowsValue() = runComposeUiTest {
        setContent { Bar(30f) }
        onNodeWithTag("pb").assert(rangeInfo(ProgressBarRangeInfo(30f, 0f..100f)))
    }

    @Test
    fun labelIsTheAccessibleName() = runComposeUiTest {
        setContent { Bar(30f) }
        onNodeWithTag("pb").assertContentDescriptionEquals("Loading")
    }

    @Test
    fun valueTextIsPercent() = runComposeUiTest {
        setContent { Bar(30f) }
        onNodeWithTag("pb").assertTextContains("30%")
    }

    @Test
    fun valueIsClampedToRange() = runComposeUiTest {
        setContent { Bar(150f) }
        onNodeWithTag("pb").assert(rangeInfo(ProgressBarRangeInfo(100f, 0f..100f)))
        onNodeWithTag("pb").assertTextContains("100%")
    }

    @Test
    fun customRangeFormatsAsFractionOfRange() = runComposeUiTest {
        setContent { Bar(2f, minValue = 0f, maxValue = 4f) }
        onNodeWithTag("pb").assert(rangeInfo(ProgressBarRangeInfo(2f, 0f..4f)))
        onNodeWithTag("pb").assertTextContains("50%")
    }

    @Test
    fun indeterminateHasNoValue() = runComposeUiTest {
        setContent { Bar(30f, indeterminate = true) }
        onNodeWithTag("pb").assert(rangeInfo(ProgressBarRangeInfo.Indeterminate))
        onNodeWithTag("pb").assertTextEquals("Loading")
    }

    private fun rangeInfo(expected: ProgressBarRangeInfo) =
        SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, expected)
}

@Composable
private fun Bar(
    value: Float,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    indeterminate: Boolean = false,
) {
    AriaProgressBar(
        value = value,
        label = "Loading",
        modifier = Modifier.testTag("pb"),
        minValue = minValue,
        maxValue = maxValue,
        isIndeterminate = indeterminate,
    ) { _, _ -> Box(Modifier) }
}
