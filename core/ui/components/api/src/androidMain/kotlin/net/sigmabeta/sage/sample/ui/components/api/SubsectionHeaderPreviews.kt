package net.sigmabeta.sage.sample.ui.components.api

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.sigmabeta.sage.sample.ui.components.api.previews.ComponentsPreview
import net.sigmabeta.sage.components.SubsectionHeaderListModel

@Preview
@Composable
private fun Light() {
    ComponentsPreview {
        Box(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Sample()
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Dark() {
    ComponentsPreview {
        Box(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Sample()
        }
    }
}

@Composable
private fun Sample() {
    SubsectionHeader(
        SubsectionHeaderListModel(
            "Sick new skills",
        ),
        modifier = Modifier,
    )
}
