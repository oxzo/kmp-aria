package dev.oxzo.aria.stately

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign

/** JavaScript `Math.round`: halves round towards positive infinity. */
private fun jsRound(x: Double): Double = floor(x + 0.5)

/**
 * Decimal places of [x] in its shortest decimal form (`0.1` → 1, `2.5e-7` → 8, `3.0` → 0).
 * react-stately's `roundToStepPrecision` derives the figure from the step's string form and
 * counts the decimal point as a digit; this counts exactly, which rounds one digit tighter and
 * never moves a value that is already on the step grid.
 */
internal fun decimalPlaces(x: Double): Int {
    if (x.isNaN() || x.isInfinite()) return 0
    val s = abs(x).toString().lowercase()
    val ePos = s.indexOf('e')
    val mantissa = if (ePos >= 0) s.substring(0, ePos) else s
    val exponent = if (ePos >= 0) s.substring(ePos + 1).toInt() else 0
    val fraction = mantissa.substringAfter('.', "").trimEnd('0')
    return max(0, fraction.length - exponent)
}

/** Rounds [value] to the decimal precision of [step] (react-stately `roundToStepPrecision`). */
fun roundToStepPrecision(value: Double, step: Double): Double {
    val places = decimalPlaces(step)
    if (places == 0) return value
    val pow = 10.0.pow(places)
    return jsRound(value * pow) / pow
}

/** react-stately `clamp`; a null bound is no bound. */
fun clamp(value: Double, min: Double?, max: Double?): Double =
    value.coerceIn(min ?: Double.NEGATIVE_INFINITY, max ?: Double.POSITIVE_INFINITY)

/**
 * react-stately `snapValueToStep`: the closest point of the grid that starts at [min] (or zero)
 * and advances by [step], kept within the bounds; when the grid overshoots [max], the last grid
 * point below it.
 */
fun snapValueToStep(value: Double, min: Double?, max: Double?, step: Double): Double {
    val remainder = (value - (min ?: 0.0)) % step
    var snapped = roundToStepPrecision(
        if (abs(remainder) * 2 >= step) value + sign(remainder) * (step - abs(remainder)) else value - remainder,
        step,
    )
    if (min != null) {
        if (snapped < min) {
            snapped = min
        } else if (max != null && snapped > max) {
            snapped = min + floor(roundToStepPrecision((max - min) / step, step)) * step
        }
    } else if (max != null && snapped > max) {
        snapped = floor(roundToStepPrecision(max / step, step)) * step
    }
    return roundToStepPrecision(snapped, step)
}

/**
 * `a + b` or `a - b`, carried out on scaled integers when either operand has decimals so that
 * `0.1 + 0.2` is `0.3` (react-stately `handleDecimalOperation`).
 */
internal fun stepArithmetic(add: Boolean, a: Double, b: Double): Double {
    if (a % 1 == 0.0 && b % 1 == 0.0) return if (add) a + b else a - b
    val pow = 10.0.pow(max(decimalPlaces(a), decimalPlaces(b)))
    val ai = jsRound(a * pow)
    val bi = jsRound(b * pow)
    return (if (add) ai + bi else ai - bi) / pow
}

/**
 * The value one step from [parsed] (react-stately `safeNextStep`): an empty field starts at the
 * minimum when incrementing and at the maximum when decrementing (zero without that bound); a
 * value off the grid snaps to it first if that already moves the wanted way, otherwise it moves
 * one [step] and snaps.
 */
fun nextStep(parsed: Double?, increment: Boolean, min: Double?, max: Double?, step: Double): Double {
    if (parsed == null) return snapValueToStep((if (increment) min else max) ?: 0.0, min, max, step)
    val snapped = snapValueToStep(parsed, min, max, step)
    if (if (increment) snapped > parsed else snapped < parsed) return snapped
    return snapValueToStep(stepArithmetic(increment, parsed, step), min, max, step)
}

/**
 * Whether a step in the given direction can still move the value (react-stately `canIncrement` /
 * `canDecrement` without their disabled and read-only terms): always from an empty field or
 * without the bound in that direction; otherwise if snapping moves the value that way, or one
 * step stays within the bound.
 */
fun canStep(parsed: Double?, increment: Boolean, min: Double?, max: Double?, step: Double): Boolean {
    if (parsed == null) return true
    val bound = (if (increment) max else min) ?: return true
    val snapped = snapValueToStep(parsed, min, max, step)
    if (if (increment) snapped > parsed else snapped < parsed) return true
    val moved = stepArithmetic(increment, parsed, step)
    return if (increment) moved <= bound else moved >= bound
}

private const val GROUP = ','
private const val DECIMAL = '.'
private val NUMBER = Regex("""^-?(\d+\.?\d*|\.\d+)$""")

/**
 * `NumberParser.isValidPartialNumber` for `en-US` in the latin numbering system: a leading minus
 * is allowed when the minimum admits negatives, a leading plus when the maximum admits positives,
 * group separators are ignored, one decimal point is allowed unless [maximumFractionDigits] is
 * zero, and nothing but digits may remain. Other numbering systems and the alternative minus
 * signs the reference sanitizes first are not handled.
 */
fun isValidPartialNumber(
    text: String,
    min: Double? = null,
    max: Double? = null,
    maximumFractionDigits: Int = 3,
): Boolean {
    var v = text
    if (v.startsWith('-') && (min == null || min < 0)) {
        v = v.drop(1)
    } else if (v.startsWith('+') && (max == null || max > 0)) {
        v = v.drop(1)
    }
    if (maximumFractionDigits == 0 && DECIMAL in v) return false
    v = v.replace(GROUP.toString(), "")
    v = v.filterNot { it in '0'..'9' }
    v = v.replaceFirst(DECIMAL.toString(), "")
    return v.isEmpty()
}

/** `NumberParser.parse` for `en-US`: group separators dropped; null where the reference returns NaN. */
fun parseNumber(text: String): Double? {
    val v = text.replace(GROUP.toString(), "").removePrefix("+")
    if (!NUMBER.matches(v)) return null
    return v.toDoubleOrNull()
}

/**
 * `Intl.NumberFormat('en-US')` with its defaults: up to [maximumFractionDigits] decimals, halves
 * rounded away from zero, trailing zeros dropped, thousands grouped with a comma. Values whose
 * whole part exceeds `Long` are not handled; a value that rounds to zero prints without a sign.
 */
fun formatNumber(value: Double, maximumFractionDigits: Int = 3): String {
    val pow = 10.0.pow(maximumFractionDigits)
    val scaled = floor(abs(value) * pow + 0.5)
    val whole = floor(scaled / pow)
    val fraction = (scaled - whole * pow).toLong()
    val grouped = whole.toLong().toString().reversed().chunked(3).joinToString(GROUP.toString()).reversed()
    val fractionText =
        if (fraction == 0L) "" else DECIMAL + fraction.toString().padStart(maximumFractionDigits, '0').trimEnd('0')
    val negative = value < 0 && scaled != 0.0
    return (if (negative) "-" else "") + grouped + fractionText
}

/**
 * Port of react-stately's `useNumberFieldState`: the committed number, the text being typed, and
 * the step, commit and validation rules of a number field. Snapshot-backed; no UI dependency.
 * Formatting and parsing are `en-US` only ([formatNumber], [parseNumber]); the hook takes an
 * `Intl.NumberFormatOptions` and a locale.
 *
 * [numberValue] is the committed number (null for empty), [parsedValue] what the current text
 * parses to; stepping and the can-step flags work from the parsed text, as the hook does. The
 * change callback fires only when the committed number changes.
 */
class NumberFieldState(
    initialValue: Double? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val step: Double? = null,
    val isDisabled: Boolean = false,
    val isReadOnly: Boolean = false,
    private val onChange: ((Double?) -> Unit)? = null,
) {
    /** The step arrows and buttons move by: [step], or 1 when none is given. */
    val clampStep: Double = step ?: 1.0

    private var current: Double? by mutableStateOf(initialValue?.let(::snapValue))

    /** The text in the field; may be a partial number the user is still typing. */
    var inputValue: String by mutableStateOf(current?.let { formatNumber(it) } ?: "")
        private set

    val numberValue: Double? get() = current

    val parsedValue: Double? get() = parseNumber(inputValue)

    val canIncrement: Boolean
        get() = !isDisabled && !isReadOnly && canStep(parsedValue, true, minValue, maxValue, clampStep)

    val canDecrement: Boolean
        get() = !isDisabled && !isReadOnly && canStep(parsedValue, false, minValue, maxValue, clampStep)

    private fun snapValue(v: Double): Double =
        if (step == null) clamp(v, minValue, maxValue) else snapValueToStep(v, minValue, maxValue, step)

    private fun set(v: Double?) {
        inputValue = v?.let { formatNumber(it) } ?: ""
        if (v != current) {
            current = v
            onChange?.invoke(v)
        }
    }

    /** Whether [text] may stand in the field while typing continues. */
    fun validate(text: String): Boolean = isValidPartialNumber(text, minValue, maxValue)

    /** Accepts [text] into the field if it is a valid partial number; returns whether it was. */
    fun setInputValue(text: String): Boolean {
        if (!validate(text)) return false
        inputValue = text
        return true
    }

    /**
     * Parses the text, clamps it to the bounds and snaps it to the step (when one is set),
     * rounds it to the format's precision, and reformats the field. An empty field commits as
     * empty; text that does not parse is replaced by the formatted committed number.
     */
    fun commit() {
        if (inputValue.isEmpty()) {
            set(null)
            return
        }
        val parsed = parseNumber(inputValue)
        if (parsed == null) {
            inputValue = current?.let { formatNumber(it) } ?: ""
            return
        }
        val snapped = snapValue(parsed)
        set(parseNumber(formatNumber(snapped)) ?: snapped)
    }

    fun increment() = set(nextStep(parsedValue, true, minValue, maxValue, clampStep))

    fun decrement() = set(nextStep(parsedValue, false, minValue, maxValue, clampStep))

    fun incrementToMax() {
        maxValue?.let { set(snapValueToStep(it, minValue, maxValue, clampStep)) }
    }

    fun decrementToMin() {
        minValue?.let { set(it) }
    }
}
