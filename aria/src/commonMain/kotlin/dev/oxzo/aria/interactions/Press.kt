package dev.oxzo.aria.interactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

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
