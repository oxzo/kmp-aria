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
 * Port of react-aria-components `ProgressBar`. Contract: `progressbar` named by its label,
 * `aria-valuenow` / `aria-valuemin` / `aria-valuemax` and `aria-valuetext` (the formatted value,
 * a percent by default); indeterminate drops the value; not focusable, no keyboard.
 *
 * Compose: `progressSemantics` sets `ProgressBarRangeInfo` (current, range, steps), the
 * vocabulary for the three value attributes; the label goes into `contentDescription`, the
 * mirror's only name path (react-aria labels through `aria-labelledby` to the rendered label
 * span). The value text comes from [formatValue], a plain rounded percent by default: the
 * reference formats through `Intl.NumberFormat` with the page locale, which the port does not
 * have, so locale-aware formatting is a deviation. The component renders the label and the value
 * text itself; [content] draws the track from the percentage.
 *
 * Reference contract: https://react-aria.adobe.com/ProgressBar
 */
@Composable
fun AriaProgressBar(
    value: Float,
    label: String,
    modifier: Modifier = Modifier,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    isIndeterminate: Boolean = false,
    valueLabel: String? = null,
    formatValue: (fraction: Float) -> String = { "${(it * 100).roundToInt()}%" },
    labelStyle: TextStyle = TextStyle.Default,
    content: @Composable (percentage: Float?, valueText: String?) -> Unit,
) {
    val clamped = value.coerceIn(minValue, maxValue)
    val range = maxValue - minValue
    val fraction = if (range == 0f) 0f else (clamped - minValue) / range
    val percentage = if (isIndeterminate) null else fraction * 100f
    val valueText = if (isIndeterminate) null else valueLabel ?: formatValue(fraction)
    val rangeSemantics = if (isIndeterminate) {
        Modifier.progressSemantics()
    } else {
        Modifier.progressSemantics(clamped, minValue..maxValue)
    }
    Column(
        modifier = modifier.then(rangeSemantics).semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(label, style = labelStyle)
            valueText?.let { BasicText(it, style = labelStyle) }
        }
        content(percentage, valueText)
    }
}
