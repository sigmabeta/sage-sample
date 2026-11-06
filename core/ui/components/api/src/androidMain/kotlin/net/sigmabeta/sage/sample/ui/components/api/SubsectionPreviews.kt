package net.sigmabeta.sage.sample.ui.components.api

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import net.sigmabeta.sage.sample.ui.components.api.previews.ComponentsPreview
import net.sigmabeta.sage.sample.ui.components.api.previews.PreviewActionSink
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.SubsectionHeaderListModel
import net.sigmabeta.sage.components.SubsectionListModel
import net.sigmabeta.sage.components.WideItemListModel
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.ui.Icon

@Preview
@Composable
private fun LightWide() {
    ComponentsPreview {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.background
                )
                .padding(16.dp)
        ) {
            SampleWide()
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DarkWide() {
    ComponentsPreview {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.background
                )
                .padding(16.dp)
        ) {
            SampleWide()
        }
    }
}

@Preview
@Composable
private fun LightSquare() {
    ComponentsPreview {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.background
                )
                .padding(16.dp)
        ) {
            SampleSquare()
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DarkSquare() {
    ComponentsPreview {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.background
                )
                .padding(16.dp)
        ) {
            SampleSquare()
        }
    }
}

@Composable
@Suppress("MagicNumber")
private fun SampleWide() {
    Subsection(
        SubsectionListModel(
            1234L,
            SubsectionHeaderListModel(
                "Composers for this game on VGLS",
            ),
            listOf(
                WideItemListModel(
                    2345L,
                    "Manami Kiyota",
                    SourceInfo(null),
                    Icon.Person,
                    actionableId = null,
                    clickAction = SageAction.Noop,
                ),
                WideItemListModel(
                    3456L,
                    "Yasunori Mitsuda",
                    SourceInfo(null),
                    Icon.Person,
                    actionableId = null,
                    clickAction = SageAction.Noop,
                ),
                WideItemListModel(
                    4567L,
                    "ACE+",
                    SourceInfo(null),
                    Icon.Person,
                    actionableId = null,
                    clickAction = SageAction.Noop,
                ),
            ).toImmutableList()
        ),
        PreviewActionSink { },
        Modifier,
        PaddingValues(horizontal = 8.dp)
    )
}

@Composable
@Suppress("MagicNumber")
private fun SampleSquare() {
    Subsection(
        SubsectionListModel(
            1234L,
            SubsectionHeaderListModel(
                "Composers for this game on VGLS",
            ),
            listOf(
                GridImageListModel(
                    2345L,
                    "Manami Kiyota",
                    SourceInfo(null),
                    Icon.Person,
                    actionableId = null,
                    clickAction = SageAction.Noop,
                ),
                GridImageListModel(
                    3456L,
                    "Yasunori Mitsuda",
                    SourceInfo(null),
                    Icon.Person,
                    actionableId = null,
                    clickAction = SageAction.Noop,
                ),
                GridImageListModel(
                    4567L,
                    "ACE+",
                    SourceInfo(null),
                    Icon.Person,
                    actionableId = null,
                    clickAction = SageAction.Noop,
                ),
            ).toImmutableList()
        ),
        PreviewActionSink { },
        Modifier,
        PaddingValues(horizontal = 8.dp)
    )
}
