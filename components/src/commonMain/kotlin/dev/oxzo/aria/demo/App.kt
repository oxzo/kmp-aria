package dev.oxzo.aria.demo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oxzo.aria.AriaButton
import dev.oxzo.aria.AriaCheckbox
import dev.oxzo.aria.AriaDisclosure
import dev.oxzo.aria.AriaLink
import dev.oxzo.aria.AriaProgressBar
import dev.oxzo.aria.AriaRadio
import dev.oxzo.aria.AriaRadioGroup
import dev.oxzo.aria.AriaRadioGroupScope
import dev.oxzo.aria.AriaSwitch
import dev.oxzo.aria.AriaTextField
import dev.oxzo.aria.AriaToggleButton
import dev.oxzo.aria.interactions.AriaDebug
import dev.oxzo.aria.interactions.focusMarker

/**
 * Demo routes. One per component, mirrored by `conformance/reference` so the Playwright
 * harness runs the same interaction script against both. Material3 control routes carry
 * the framework's own widgets so a missing attribute can be attributed to the framework
 * rather than the port.
 */
val demoRoutes: List<String> = listOf(
    "/button",
    "/toggle-button",
    "/checkbox",
    "/switch",
    "/radio-group",
    "/text-field",
    "/link",
    "/progress-bar",
    "/disclosure",
    "/m3-button",
    "/m3-toggle-button",
    "/m3-checkbox",
    "/m3-switch",
    "/m3-radio",
    "/m3-text-field",
    "/fw-link",
    "/m3-progress-bar",
)

@Composable
fun App(route: String) {
    AriaDebug.focusMarker = true
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (route.removePrefix("#")) {
                "/button" -> ButtonDemo()
                "/toggle-button" -> ToggleButtonDemo()
                "/checkbox" -> CheckboxDemo()
                "/switch" -> SwitchDemo()
                "/radio-group" -> RadioGroupDemo()
                "/text-field" -> TextFieldDemo()
                "/link" -> LinkDemo()
                "/progress-bar" -> ProgressBarDemo()
                "/disclosure" -> DisclosureDemo()
                "/fw-link" -> FoundationLinkDemo()
                "/m3-progress-bar" -> M3ProgressBarDemo()
                "/m3-button" -> M3ButtonDemo()
                "/m3-checkbox" -> M3CheckboxDemo()
                "/m3-radio" -> M3RadioDemo()
                "/m3-text-field" -> M3TextFieldDemo()
                "/m3-switch" -> M3SwitchDemo()
                "/m3-toggle-button" -> M3ToggleButtonDemo()
                else -> Index()
            }
        }
    }
}

private val label = TextStyle(fontSize = 16.sp, color = Color.Black)

@Composable
private fun Index() {
    BasicText("kmp-aria demo", style = label)
    demoRoutes.forEach { BasicText("#$it", style = label) }
}

@Composable
private fun ButtonDemo() {
    var count by remember { mutableStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AriaButton(
            onPress = { count++ },
            modifier = Modifier.testTag("btn").focusMarker("Press me").border(1.dp, Color.Black),
        ) {
            BasicText("Press me", modifier = Modifier.padding(8.dp), style = label)
        }
        AriaButton(
            onPress = { count++ },
            enabled = false,
            modifier = Modifier.testTag("btn-disabled").border(1.dp, Color.Gray),
        ) {
            BasicText("Disabled", modifier = Modifier.padding(8.dp), style = label)
        }
    }
    BasicText("Pressed $count times", modifier = Modifier.testTag("count"), style = label)
}

@Composable
private fun ToggleButtonDemo() {
    var selected by remember { mutableStateOf(false) }
    AriaToggleButton(
        isSelected = selected,
        onChange = { selected = it },
        modifier = Modifier.testTag("tb").focusMarker("Bold").border(1.dp, Color.Black),
    ) {
        BasicText("Bold", modifier = Modifier.padding(8.dp), style = label)
    }
    BasicText(if (selected) "Selected" else "Not selected", modifier = Modifier.testTag("state"), style = label)
}

@Composable
private fun CheckboxDemo() {
    var selected by remember { mutableStateOf(false) }
    LabelledCheckbox("Subscribe", tag = "cb", selected = selected, onChange = { selected = it })
    LabelledCheckbox("Select all", tag = "cb-mixed", selected = false, onChange = {}, indeterminate = true)
    LabelledCheckbox("Disabled", tag = "cb-disabled", selected = false, onChange = {}, enabled = false)
    BasicText(if (selected) "Selected" else "Not selected", modifier = Modifier.testTag("state"), style = label)
}

@Composable
private fun LabelledCheckbox(
    text: String,
    tag: String,
    selected: Boolean,
    onChange: (Boolean) -> Unit,
    indeterminate: Boolean = false,
    enabled: Boolean = true,
) {
    AriaCheckbox(
        isSelected = selected,
        onChange = onChange,
        modifier = Modifier.testTag(tag).focusMarker(text),
        isIndeterminate = indeterminate,
        enabled = enabled,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CheckIndicator(
                when {
                    indeterminate -> ToggleableState.Indeterminate
                    selected -> ToggleableState.On
                    else -> ToggleableState.Off
                },
            )
            BasicText(text, style = label)
        }
    }
}

/** Drawn, not text, so the glyph does not merge into the accessible name. */
@Composable
private fun CheckIndicator(state: ToggleableState) {
    Box(
        Modifier.size(16.dp).border(1.dp, Color.Black).drawBehind {
            val inset = 3.dp.toPx()
            when (state) {
                ToggleableState.On -> drawRect(
                    Color.Black,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2 * inset, size.height - 2 * inset),
                )
                ToggleableState.Indeterminate -> drawRect(
                    Color.Black,
                    topLeft = Offset(inset, size.height / 2 - 1.dp.toPx()),
                    size = Size(size.width - 2 * inset, 2.dp.toPx()),
                )
                ToggleableState.Off -> Unit
            }
        },
    )
}

@Composable
private fun SwitchDemo() {
    var on by remember { mutableStateOf(false) }
    AriaSwitch(
        isSelected = on,
        onChange = { on = it },
        modifier = Modifier.testTag("sw").focusMarker("Wi-Fi"),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp, 16.dp).border(1.dp, Color.Black).drawBehind {
                    val r = 5.dp.toPx()
                    drawCircle(Color.Black, radius = r, center = Offset(if (on) size.width - r - 3.dp.toPx() else r + 3.dp.toPx(), size.height / 2))
                },
            )
            BasicText("Wi-Fi", style = label)
        }
    }
    BasicText(if (on) "On" else "Off", modifier = Modifier.testTag("state"), style = label)
}

private val pets = listOf("dog" to "Dog", "cat" to "Cat", "dragon" to "Dragon")

@Composable
private fun RadioGroupDemo() {
    var pet by remember { mutableStateOf<String?>(null) }
    AriaRadioGroup(
        value = pet,
        onChange = { pet = it },
        label = "Favorite pet",
        modifier = Modifier.testTag("group"),
        labelStyle = label,
    ) {
        pets.forEach { (value, text) -> PetRadio(value, text) }
    }
    BasicText("Selected: ${pet ?: "none"}", modifier = Modifier.testTag("state"), style = label)
}

@Composable
private fun AriaRadioGroupScope.PetRadio(value: String, text: String) {
    val selected = value == selectedValue
    AriaRadio(value, modifier = Modifier.testTag("r-$value").focusMarker(text)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(16.dp).drawBehind {
                    drawCircle(Color.Black, style = Stroke(1.dp.toPx()))
                    if (selected) drawCircle(Color.Black, radius = 4.dp.toPx())
                },
            )
            BasicText(text, style = label)
        }
    }
}

@Composable
private fun TextFieldDemo() {
    var name by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    AriaTextField(
        value = name,
        onValueChange = { name = it },
        label = "Name",
        modifier = Modifier.testTag("tf").focusMarker("Name").border(1.dp, Color.Black).padding(4.dp),
        textStyle = label,
    )
    AriaTextField(
        value = secret,
        onValueChange = { secret = it },
        label = "Password",
        modifier = Modifier.testTag("pw").focusMarker("Password").border(1.dp, Color.Black).padding(4.dp),
        secure = true,
        textStyle = label,
    )
    BasicText("Value: $name", modifier = Modifier.testTag("state"), style = label)
}

private val linkStyle = label.copy(textDecoration = TextDecoration.Underline)

@Composable
private fun LinkDemo() {
    var count by remember { mutableStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AriaLink(onPress = { count++ }, modifier = Modifier.testTag("lnk").focusMarker("Follow me")) {
            BasicText("Follow me", style = linkStyle)
        }
        AriaLink(href = "https://react-aria.adobe.com/Link", modifier = Modifier.testTag("lnk-href").focusMarker("Docs")) {
            BasicText("Docs", style = linkStyle)
        }
        AriaLink(onPress = { count++ }, enabled = false, modifier = Modifier.testTag("lnk-disabled")) {
            BasicText("Disabled", style = label.copy(color = Color.Gray))
        }
    }
    BasicText("Followed $count times", modifier = Modifier.testTag("count"), style = label)
}

/**
 * Framework control for Link: Material3 has no link widget, so the control is the framework's
 * own link mechanism, a `LinkAnnotation.Clickable` inside `BasicText` (foundation, not
 * Material3). It is what `jb-main`'s `LinkTestMarker` → `role="link"` mapping applies to.
 */
@Composable
private fun FoundationLinkDemo() {
    var count by remember { mutableStateOf(0) }
    val text = buildAnnotatedString {
        withLink(
            LinkAnnotation.Clickable(
                tag = "follow",
                styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                linkInteractionListener = { count++ },
            ),
        ) {
            append("Follow me")
        }
    }
    BasicText(text, modifier = Modifier.testTag("lnk"), style = label)
    BasicText("Followed $count times", modifier = Modifier.testTag("count"), style = label)
}

@Composable
private fun ProgressBarDemo() {
    var value by remember { mutableStateOf(30f) }
    AriaProgressBar(value = value, label = "Loading", modifier = Modifier.testTag("pb"), labelStyle = label) { percentage, _ ->
        Track(percentage)
    }
    AriaButton(
        onPress = { value = (value + 30f).coerceAtMost(100f) },
        modifier = Modifier.testTag("adv").focusMarker("Advance").border(1.dp, Color.Black),
    ) {
        BasicText("Advance", modifier = Modifier.padding(8.dp), style = label)
    }
    AriaProgressBar(value = 0f, label = "Syncing", modifier = Modifier.testTag("pb-ind"), isIndeterminate = true, labelStyle = label) { percentage, _ ->
        Track(percentage)
    }
    BasicText("Value: ${value.toInt()}", modifier = Modifier.testTag("state"), style = label)
}

/** Drawn track; an indeterminate bar (null percentage) shows a fixed 40 % segment. */
@Composable
private fun Track(percentage: Float?) {
    Box(
        Modifier.size(200.dp, 12.dp).border(1.dp, Color.Black).drawBehind {
            val fill = (percentage ?: 40f) / 100f
            drawRect(Color.Black, size = Size(size.width * fill, size.height))
        },
    )
}

@Composable
private fun DisclosureDemo() {
    var open by remember { mutableStateOf(false) }
    AriaDisclosure(
        isExpanded = open,
        onExpandedChange = { open = it },
        modifier = Modifier.testTag("disc"),
        triggerModifier = Modifier.testTag("trig").focusMarker("System Requirements").border(1.dp, Color.Black),
        panelModifier = Modifier.testTag("panel").padding(8.dp),
        trigger = { BasicText("System Requirements", modifier = Modifier.padding(8.dp), style = label) },
    ) {
        BasicText("Details about system requirements here.", style = label)
    }
    BasicText(if (open) "Expanded" else "Collapsed", modifier = Modifier.testTag("state"), style = label)
}

@Composable
private fun M3ProgressBarDemo() {
    var value by remember { mutableStateOf(30f) }
    Text("Loading")
    LinearProgressIndicator(progress = { value / 100f }, modifier = Modifier.testTag("pb"))
    Button(onClick = { value = (value + 30f).coerceAtMost(100f) }, modifier = Modifier.testTag("adv")) { Text("Advance") }
    Text("Value: ${value.toInt()}", modifier = Modifier.testTag("state"))
}

@Composable
private fun M3ButtonDemo() {
    var count by remember { mutableStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { count++ }, modifier = Modifier.testTag("btn")) { Text("Press me") }
        Button(onClick = { count++ }, enabled = false, modifier = Modifier.testTag("btn-disabled")) { Text("Disabled") }
    }
    Text("Pressed $count times", modifier = Modifier.testTag("count"))
}

@Composable
private fun M3CheckboxDemo() {
    var checked by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { checked = it }, modifier = Modifier.testTag("cb"))
        Text("Subscribe")
    }
    Text(if (checked) "Selected" else "Not selected", modifier = Modifier.testTag("state"))
}

@Composable
private fun M3RadioDemo() {
    var pet by remember { mutableStateOf<String?>(null) }
    Column(Modifier.selectableGroup()) {
        Text("Favorite pet")
        pets.forEach { (value, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = pet == value, onClick = { pet = value }, modifier = Modifier.testTag("r-$value"))
                Text(text)
            }
        }
    }
    Text("Selected: ${pet ?: "none"}", modifier = Modifier.testTag("state"))
}

@Composable
private fun M3TextFieldDemo() {
    var name by remember { mutableStateOf("") }
    TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.testTag("tf"))
    Text("Value: $name", modifier = Modifier.testTag("state"))
}

@Composable
private fun M3SwitchDemo() {
    var checked by remember { mutableStateOf(false) }
    Switch(checked = checked, onCheckedChange = { checked = it }, modifier = Modifier.testTag("sw"))
    Text(if (checked) "On" else "Off", modifier = Modifier.testTag("state"))
}

@Composable
private fun M3ToggleButtonDemo() {
    var checked by remember { mutableStateOf(false) }
    IconToggleButton(checked = checked, onCheckedChange = { checked = it }, modifier = Modifier.testTag("tb")) {
        Text(if (checked) "\u2605" else "\u2606")
    }
    Text(if (checked) "Selected" else "Not selected", modifier = Modifier.testTag("state"))
}
