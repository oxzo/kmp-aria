package dev.oxzo.aria

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Semantics-tree instrument for TextField. The react-aria contract under test: the label is
 * the accessible name, typing updates the value, disabled, read-only, invalid, password.
 */
@OptIn(ExperimentalTestApi::class)
class TextFieldTest {
    @Test
    fun typingUpdatesValue() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("tf").performTextInput("Ada")
        onNodeWithTag("value").assertTextEquals("Value: Ada")
    }

    @Test
    fun labelIsTheAccessibleName() = runComposeUiTest {
        setContent { Field() }
        onNodeWithTag("tf").assertContentDescriptionEquals("Name")
    }

    @Test
    fun disabledIsNotEnabled() = runComposeUiTest {
        setContent { Field(enabled = false) }
        onNodeWithTag("tf").assertIsNotEnabled()
    }

    @Test
    fun readOnlyIsNotEditable() = runComposeUiTest {
        setContent { Field(readOnly = true) }
        onNodeWithTag("tf").assert(SemanticsMatcher.expectValue(SemanticsProperties.IsEditable, false))
    }

    @Test
    fun invalidExposesError() = runComposeUiTest {
        setContent { Field(invalid = true) }
        onNodeWithTag("tf").assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Too short"))
    }

    @Test
    fun secureIsPassword() = runComposeUiTest {
        setContent { Field(secure = true) }
        onNodeWithTag("tf").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }
}

@Composable
private fun Field(
    enabled: Boolean = true,
    readOnly: Boolean = false,
    invalid: Boolean = false,
    secure: Boolean = false,
) {
    var value by remember { mutableStateOf("") }
    Column {
        AriaTextField(
            value = value,
            onValueChange = { value = it },
            label = "Name",
            modifier = Modifier.testTag("tf"),
            enabled = enabled,
            readOnly = readOnly,
            invalid = invalid,
            secure = secure,
            errorMessage = if (invalid) "Too short" else null,
        )
        BasicText("Value: $value", modifier = Modifier.testTag("value"))
    }
}
