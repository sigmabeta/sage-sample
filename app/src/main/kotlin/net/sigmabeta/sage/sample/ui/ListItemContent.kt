package net.sigmabeta.sage.sample.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.DropdownSettingListModel
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.LabelValueListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.NameCaptionListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.vector

/**
 * The app's renderer for SAGE [ListModel]s.
 *
 * SAGE owns the screen scaffolding (state, scrolling, insets, pagination); the app owns the
 * pixel rendering of each model type. The loading, empty and error models SAGE generates from
 * `LCE` states have explicit presentations; only genuinely unknown feature models fall through
 * to a spacer so feature modules can introduce their own model types without breaking the app.
 */
@Composable
fun ListItemContent(
    model: ListModel,
    sink: ActionSink,
    modifier: Modifier,
    padding: PaddingValues,
) {
    when (model) {
        is SectionHeaderListModel -> Text(
            text = model.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(top = 16.dp, bottom = 8.dp),
        )

        is SingleTextListModel -> Text(
            text = model.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (model.active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (model.active) FontWeight.Medium else FontWeight.Normal,
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = model.clickAction !is SageAction.Noop) {
                    sink.sendAction(model.clickAction)
                }
                .padding(padding)
                .padding(vertical = 8.dp),
        )

        is NameCaptionListModel -> Column(
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = model.clickAction !is SageAction.Noop) {
                    sink.sendAction(model.clickAction)
                }
                .padding(padding)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (model.active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (model.active) FontWeight.Medium else FontWeight.Normal,
            )
            Text(
                text = model.caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Rows whose image is a `generated://` URL come from `:core:bitmaps`' Coil fetcher, so
        // AsyncImage resolves the artwork through the singleton loader. A non-URL source (or no
        // source) falls back to the model's placeholder icon.
        is ImageNameCaptionListModel -> Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = model.clickAction !is SageAction.Noop) {
                    sink.sendAction(model.clickAction)
                }
                .padding(padding)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val imageUrl = model.sourceInfo.info as? String
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = model.imagePlaceholder.vector(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (model.active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (model.active) FontWeight.Medium else FontWeight.Normal,
                )
                Text(
                    text = model.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Collapsed, a dropdown reads like a label/value row with a trailing caret. Expanding it
        // renders each option through this same dispatcher, so an option can be any ListModel.
        is DropdownSettingListModel -> {
            val caretRotation by animateFloatAsState(
                targetValue = if (model.expanded) 180f else 0f,
                label = "DropdownSettingListModel.caretRotation",
            )
            Column(modifier = modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = model.onExpandClicked !is SageAction.Noop) {
                            sink.sendAction(model.onExpandClicked)
                        }
                        .padding(padding)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = model.options.getOrNull(model.selectedPosition)?.first.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icon.Caret.vector(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(caretRotation),
                    )
                }

                if (model.expanded) {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        model.options.forEach { (_, option) ->
                            ListItemContent(option, sink, Modifier, padding)
                        }
                    }
                }
            }
        }

        // Display-only About rows use SageAction.Noop and are therefore not clickable.
        is LabelValueListModel -> Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = model.clickAction !is SageAction.Noop) {
                    sink.sendAction(model.clickAction)
                }
                .padding(padding)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = model.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            model.value?.let { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is LoadingItemListModel -> Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(vertical = 8.dp)
                .height(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ),
        )

        // SAGE's empty state carries an icon, so render it beside the explanation instead of
        // dropping to text-only presentation.
        is EmptyStateListModel -> Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = model.icon.vector(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = model.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is ErrorStateListModel -> Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = model.errorString,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = model.failedOperationName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        else -> Spacer(modifier = modifier)
    }
}
