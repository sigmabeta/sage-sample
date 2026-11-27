package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.sample.ui.components.api.previews.ComponentsPreview
import net.sigmabeta.sage.sample.ui.components.models.ButtonListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageTone
import net.sigmabeta.sage.sample.ui.components.models.TextFieldListModel
import net.sigmabeta.sage.ui.composables.previews.PreviewActionSink

@Preview
@Composable
private fun FilledForm() {
    ComponentsPreview {
        Form(enabled = true)
    }
}

@Preview
@Composable
private fun DisabledForm() {
    ComponentsPreview(forceDark = true) {
        Form(enabled = false)
    }
}

@Composable
private fun Form(enabled: Boolean) {
    val sink = PreviewActionSink { }
    val padding = PaddingValues(horizontal = 16.dp)
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        TextFieldListItem(
            TextFieldListModel(id = 1L, label = "Username", text = "demo", enabled = enabled),
            sink,
            Modifier,
            padding,
        )
        TextFieldListItem(
            TextFieldListModel(id = 2L, label = "Password", text = "sage", masked = true, enabled = enabled),
            sink,
            Modifier,
            padding,
        )
        FormMessageListItem(FormMessageListModel("Something went wrong.", FormMessageTone.ERROR, dataId = 3L), Modifier, padding)
        ButtonListItem(ButtonListModel("Continue", SageAction.Noop, enabled = enabled, dataId = 4L), sink, Modifier, padding)
        FormMessageListItem(FormMessageListModel("A hint under the form.", FormMessageTone.INFO, dataId = 5L), Modifier, padding)
    }
}
