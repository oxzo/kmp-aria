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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Semantics-tree instrument for Meter. The react-aria contract under test: the label is the
 * accessible name, the range info follows the value within its range, the value text is a
 * percent of the range unless a value label is given, out-of-range values clamp.
 */
@OptIn(ExperimentalTestApi::class)
class MeterTest {
    @Test
    fun rangeInfoFollowsValue() = runComposeUiTest {
        setContent { Meter(25f) }
        onNodeWithTag("meter").assert(rangeInfo(ProgressBarRangeInfo(25f, 0f..100f)))
    }

    @Test
    fun labelIsTheAccessibleName() = runComposeUiTest {
        setContent { Meter(25f) }
        onNodeWithTag("meter").assertContentDescriptionEquals("Storage space")
    }

    @Test
    fun valueTextIsPercent() = runComposeUiTest {
        setContent { Meter(25f) }
        onNodeWithTag("meter").assertTextContains("25%")
    }

    @Test
    fun valueIsClampedToRange() = runComposeUiTest {
        setContent { Meter(150f) }
        onNodeWithTag("meter").assert(rangeInfo(ProgressBarRangeInfo(100f, 0f..100f)))
        onNodeWithTag("meter").assertTextContains("100%")
    }

    @Test
    fun customRangeFormatsAsFractionOfRange() = runComposeUiTest {
        setContent { Meter(2f, minValue = 0f, maxValue = 4f) }
        onNodeWithTag("meter").assert(rangeInfo(ProgressBarRangeInfo(2f, 0f..4f)))
        onNodeWithTag("meter").assertTextContains("50%")
    }

    @Test
    fun valueLabelReplacesThePercent() = runComposeUiTest {
        setContent { Meter(25f, valueLabel = "1 of 4 GB") }
        onNodeWithTag("meter").assertTextContains("1 of 4 GB")
    }

    private fun rangeInfo(expected: ProgressBarRangeInfo) =
        SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, expected)
}

@Composable
private fun Meter(
    value: Float,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    valueLabel: String? = null,
) {
    AriaMeter(
        value = value,
        label = "Storage space",
        modifier = Modifier.testTag("meter"),
        minValue = minValue,
        maxValue = maxValue,
        valueLabel = valueLabel,
    ) { _, _ -> Box(Modifier) }
}
