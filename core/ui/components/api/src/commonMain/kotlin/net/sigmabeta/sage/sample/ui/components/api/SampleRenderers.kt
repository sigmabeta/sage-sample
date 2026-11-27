package net.sigmabeta.sage.sample.ui.components.api

import net.sigmabeta.sage.sample.bitmaps.GeneratedBitmap
import net.sigmabeta.sage.sample.ui.components.models.ButtonListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageListModel
import net.sigmabeta.sage.sample.ui.components.models.TextFieldListModel
import net.sigmabeta.sage.ui.composables.SageRenderers

/**
 * The sample's renderer configuration for SAGE's shared renderer suite.
 *
 * SAGE's `ListModel.Content` consults `contentFor` before its built-in renderers, so the sample's
 * form rows — [TextFieldListModel], [ButtonListModel] and [FormMessageListModel], the three branches
 * the old `ComposableMapping` handled — still render through the shared entry point. Every other
 * model falls through to SAGE's built-ins.
 *
 * `previewImage` plugs the sample's `:core:bitmaps` generated art back into SAGE's inspection-mode
 * stand-in, so the Paparazzi goldens stay byte-identical to the old `subs/FakeImage`.
 */
val SampleRenderers = SageRenderers(
    contentFor = { model ->
        when (model) {
            is TextFieldListModel -> { _, sink, _, modifier, padding ->
                TextFieldListItem(model = model, actionSink = sink, modifier = modifier, padding = padding)
            }

            is ButtonListModel -> { _, sink, _, modifier, padding ->
                ButtonListItem(model = model, actionSink = sink, modifier = modifier, padding = padding)
            }

            is FormMessageListModel -> { _, _, _, modifier, padding ->
                FormMessageListItem(model = model, modifier = modifier, padding = padding)
            }

            else -> null
        }
    },
    previewImage = { sourceInfo, modifier ->
        GeneratedBitmap(sourceInfo = sourceInfo.info.toString(), modifier = modifier)
    },
)
