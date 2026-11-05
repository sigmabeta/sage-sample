package net.sigmabeta.sage.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.SingleTextListModel

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
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = model.clickAction !is SageAction.Noop) {
                    sink.sendAction(model.clickAction)
                }
                .padding(padding)
                .padding(vertical = 8.dp),
        )

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

        is EmptyStateListModel -> Text(
            text = model.explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(vertical = 16.dp),
        )

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
