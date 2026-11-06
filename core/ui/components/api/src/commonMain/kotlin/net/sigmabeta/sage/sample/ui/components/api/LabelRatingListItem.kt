package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import net.sigmabeta.sage.sample.ui.components.api.subs.LabeledThingy
import net.sigmabeta.sage.sample.ui.components.api.subs.Rating
import net.sigmabeta.sage.sample.ui.components.api.ComponentStringId
import net.sigmabeta.sage.sample.ui.components.api.text
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.components.LabelRatingStarListModel

@Composable
fun LabelRatingListItem(
    model: LabelRatingStarListModel,
    actionSink: ActionSink,
    modifier: Modifier,
    padding: PaddingValues,
) {
    LabeledThingy(
        label = model.label,
        thingy = {
            Rating(
                score = model.value,
                modifier = Modifier
            )
        },
        onClick = { actionSink.sendAction(model.clickAction) },
        onClickLabel = ComponentStringId.ACCY_OCL_RATING.text(),
        modifier = modifier.semantics {
            stateDescription = "${model.value} out of 4"
        },
        padding = padding,
    )
}
