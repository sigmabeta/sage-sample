package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.sample.ui.components.models.ButtonListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageTone
import net.sigmabeta.sage.sample.ui.components.models.TextFieldChanged
import net.sigmabeta.sage.sample.ui.components.models.TextFieldDone
import net.sigmabeta.sage.sample.ui.components.models.TextFieldListModel

private val FORM_ROW_VERTICAL_PADDING = 8.dp

/**
 * Renderer for a [TextFieldListModel]: a full-width outlined field with a floating label.
 *
 * The typed text is held here so typing echoes on the same frame; every change is also sent as
 * [TextFieldChanged]. The text is seeded from [TextFieldListModel.text] on first render and again
 * whenever [TextFieldListModel.resetKey] changes. A masked field draws bullets and asks for a
 * password keyboard; the keyboard action is Done ([TextFieldDone]) or Next (move focus on).
 */
@Composable
fun TextFieldListItem(
    model: TextFieldListModel,
    actionSink: ActionSink,
    modifier: Modifier,
    padding: PaddingValues,
) {
    // TextFieldValue (not a bare String) so a seeded value puts the cursor at its end.
    var fieldValue by remember(model.id, model.resetKey) {
        mutableStateOf(TextFieldValue(model.text, TextRange(model.text.length)))
    }
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { value ->
            val changed = value.text != fieldValue.text
            fieldValue = value
            if (changed) actionSink.sendAction(TextFieldChanged(model.id, value.text))
        },
        label = { Text(text = model.label) },
        enabled = model.enabled,
        singleLine = true,
        visualTransformation = if (model.masked) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (model.masked) KeyboardType.Password else KeyboardType.Text,
            autoCorrectEnabled = false,
            imeAction = if (model.submitOnDone) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onDone = { actionSink.sendAction(TextFieldDone(model.id)) },
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(vertical = FORM_ROW_VERTICAL_PADDING),
    )
}

/** Renderer for a [ButtonListModel]: a full-width filled button, greyed out when disabled. */
@Composable
fun ButtonListItem(
    model: ButtonListModel,
    actionSink: ActionSink,
    modifier: Modifier,
    padding: PaddingValues,
) {
    Button(
        onClick = { actionSink.sendAction(model.clickAction) },
        enabled = model.enabled,
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(vertical = FORM_ROW_VERTICAL_PADDING),
    ) {
        Text(text = model.label)
    }
}

/** Renderer for a [FormMessageListModel]: one line of body text, in the error colour for errors. */
@Composable
fun FormMessageListItem(
    model: FormMessageListModel,
    modifier: Modifier,
    padding: PaddingValues,
) {
    Text(
        text = model.text,
        style = MaterialTheme.typography.bodyMedium,
        color = when (model.tone) {
            FormMessageTone.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
            FormMessageTone.ERROR -> MaterialTheme.colorScheme.error
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(vertical = FORM_ROW_VERTICAL_PADDING),
    )
}
