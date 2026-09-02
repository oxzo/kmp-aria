package dev.oxzo.aria.demo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oxzo.aria.AriaButton
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
    "/m3-button",
    "/m3-switch",
    "/m3-toggle-button",
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
                "/m3-button" -> M3ButtonDemo()
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
private fun M3ButtonDemo() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }, modifier = Modifier.testTag("btn")) { Text("Press me") }
    Text("Pressed $count times", modifier = Modifier.testTag("count"))
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
        Icon(
            imageVector = if (checked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Favorite",
        )
    }
    Text(if (checked) "Selected" else "Not selected", modifier = Modifier.testTag("state"))
}
