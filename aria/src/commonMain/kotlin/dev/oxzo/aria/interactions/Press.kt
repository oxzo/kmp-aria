package dev.oxzo.aria.interactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState

/**
 * Port of react-aria's `usePress` contract for a single activation: pointer press, Enter and
 * Space from the keyboard, focusable when enabled, inert when disabled.
 *
 * Compose's `clickable` already carries that contract on every platform (pointer, Enter,
 * NumPadEnter, Space; `focusable(enabled)`; `Role` + `OnClick` semantics; `disabled()` when
 * not enabled). This modifier names it so the behaviour layer has one place to diverge from
 * the primitive when a react-aria detail (Space-on-keyup, press cancel on pointer leave)
 * turns out to matter for a component.
 */
fun Modifier.ariaPressable(
    onPress: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    interactionSource: MutableInteractionSource? = null,
): Modifier = this.clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    role = role,
    onClick = onPress,
)

/**
 * Port of react-aria's `useToggleButton` interaction: a pressable whose semantics carry a
 * [androidx.compose.ui.state.ToggleableState] instead of a bare click.
 *
 * On the web target the framework does not mirror `toggleableState` into the accessibility
 * DOM, so `aria-pressed` never reaches the browser. That is the first measured gap of the
 * project; the semantics are set regardless so the Compose-side instrument can assert them.
 */
fun Modifier.ariaToggleable(
    isSelected: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    interactionSource: MutableInteractionSource? = null,
): Modifier = this.toggleable(
    value = isSelected,
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    role = role,
    onValueChange = onChange,
)

/**
 * Port of react-aria's `useToggle` as Checkbox and Switch use it: a toggleable whose keyboard
 * activation is Space only. react-aria's `usePress` lets Enter through for buttons but not for
 * checkbox and radio inputs (`usePress.ts`, `isValidInputKey`: "Only space should toggle
 * checkboxes and radios, not enter"), which is the native input contract. Compose's
 * `toggleable` treats Enter and Space alike, so Enter is consumed before it reaches the
 * primitive; the browser instrument sees the difference as the state text after the Enter step.
 *
 * `readOnly` keeps the node focusable and makes the toggle a no-op: Compose has no read-only
 * semantics property for toggleables, so `aria-readonly` has nothing to cross on any platform.
 * `invalid` sets the `Error` semantics property, the Compose vocabulary for `aria-invalid`.
 */
fun Modifier.ariaCheckable(
    state: ToggleableState,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
    role: Role = Role.Checkbox,
    interactionSource: MutableInteractionSource? = null,
): Modifier = this
    .spaceOnlyActivation()
    .then(if (invalid) Modifier.semantics { error("Invalid") } else Modifier)
    .triStateToggleable(
        state = state,
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        role = role,
        onClick = { if (!readOnly) onToggle() },
    )

/**
 * Consumes Enter and NumPadEnter (down and up) before the clickable primitive sees them, so
 * only Space activates. Place before the primitive in the chain: preview key events travel
 * from the outer modifier inward.
 */
internal fun Modifier.spaceOnlyActivation(): Modifier = this.onPreviewKeyEvent {
    it.key == Key.Enter || it.key == Key.NumPadEnter
}

/**
 * The mirror image for links: consumes Space (down and up) before the clickable primitive sees
 * it, so only Enter activates. react-aria's `usePress` (`isValidKeyboardEvent`: "Links should
 * only trigger with Enter key") applies this to `role="link"` and to anchors; on a native `<a>`
 * Space scrolls the page. Place before the primitive in the chain.
 */
internal fun Modifier.enterOnlyActivation(): Modifier = this.onPreviewKeyEvent {
    it.key == Key.Spacebar
}
