package dev.oxzo.aria

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Port of react-aria-components `Meter`. Contract (`useMeter`, which is `useProgressBar` with the
 * role replaced): `role="meter progressbar"` (the meter role, with progressbar as the fallback
 * token for browsers without it) named by its label, `aria-valuenow` / `aria-valuemin` /
 * `aria-valuemax` and `aria-valuetext` (the formatted value, a percent by default), the value
 * clamped to the range; no indeterminate state, not focusable, no keyboard.
 *
 * Compose: the same `progressSemantics` as [AriaProgressBar], since Compose's range vocabulary
 * (`ProgressBarRangeInfo`) has no meter/progress distinction and no role for either; the label
 * goes into `contentDescription`. Value text as in [AriaProgressBar]: a rounded percent unless
 * [valueLabel] is given, where the reference formats through `Intl.NumberFormat`.
 *
 * Reference contract: https://react-aria.adobe.com/Meter
 */
@Composable
fun AriaMeter(
    value: Float,
    label: String,
    modifier: Modifier = Modifier,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    valueLabel: String? = null,
    formatValue: (fraction: Float) -> String = { "${(it * 100).roundToInt()}%" },
    labelStyle: TextStyle = TextStyle.Default,
    content: @Composable (percentage: Float, valueText: String) -> Unit,
) {
    val clamped = value.coerceIn(minValue, maxValue)
    val range = maxValue - minValue
    val fraction = if (range == 0f) 0f else (clamped - minValue) / range
    val valueText = valueLabel ?: formatValue(fraction)
    Column(
        modifier = modifier.progressSemantics(clamped, minValue..maxValue).semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(label, style = labelStyle)
            BasicText(valueText, style = labelStyle)
        }
        content(fraction * 100f, valueText)
    }
}
