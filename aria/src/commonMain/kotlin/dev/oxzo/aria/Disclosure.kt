package dev.oxzo.aria

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * Port of react-aria-components `Disclosure` with its `Heading`, `Button slot="trigger"` and
 * `DisclosurePanel`. Contract: a heading wrapping a button that carries `aria-expanded` and
 * `aria-controls` and toggles on press, Enter and Space (it is a button, so both keys); a panel
 * with `role="group"` labelled by the trigger, hidden while collapsed.
 *
 * Compose: `heading()` on the wrapper, the button through [AriaButton], and the `expand` /
 * `collapse` semantics actions on the trigger, which is the Compose vocabulary for the expanded
 * state (there is no boolean property; the action that is present says which state the node is
 * in). The panel is not composed while collapsed, where the reference keeps it in the DOM as
 * `hidden="until-found"` so find-in-page can reach it. Compose has no group role, so the panel
 * carries none, and no heading level exists in Compose semantics.
 *
 * Reference contract: https://react-aria.adobe.com/Disclosure
 */
@Composable
fun AriaDisclosure(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    triggerModifier: Modifier = Modifier,
    panelModifier: Modifier = Modifier,
    enabled: Boolean = true,
    trigger: @Composable () -> Unit,
    panel: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.semantics { heading() }) {
            AriaButton(
                onPress = { onExpandedChange(!isExpanded) },
                modifier = triggerModifier.semantics {
                    if (isExpanded) {
                        collapse {
                            onExpandedChange(false)
                            true
                        }
                    } else {
                        expand {
                            onExpandedChange(true)
                            true
                        }
                    }
                },
                enabled = enabled,
            ) {
                trigger()
            }
        }
        if (isExpanded) {
            Column(modifier = panelModifier) { panel() }
        }
    }
}
