package net.sigmabeta.sage.sample.feature.componentlibrary

import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.ui.components.models.ButtonListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageListModel
import net.sigmabeta.sage.sample.ui.components.models.FormMessageTone
import net.sigmabeta.sage.sample.ui.components.models.TextFieldListModel
import net.sigmabeta.sage.ui.StringProvider
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryConfig
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.ui.componentlibrary.componentLibraryItems

/**
 * The Component Library's content: SAGE's catalog for [config], followed by the sample's own
 * form rows under the catalog's "App rows" header.
 */
data class ComponentLibraryState(
    val config: ComponentLibraryConfig = ComponentLibraryConfig(),
) : ListState() {
    override val columnType = config.layout.columnType

    override fun title(stringProvider: StringProvider) = TitleBarModel(
        title = stringProvider.getString(SampleStringId.NAV_COMPONENT_LIBRARY),
        shouldShowBack = true,
    )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        componentLibraryItems(config, extraSamples = sampleFormRows(stringProvider))
}

/**
 * One of each sample-owned row from `:core:ui:components:models`, rendered through
 * `SampleRenderers.contentFor`. Their ids sit clear of the catalog's (which uses 1,000,000+ and
 * string hashes).
 */
internal fun sampleFormRows(stringProvider: StringProvider): List<ListModel> = listOf(
    TextFieldListModel(
        id = TEXT_FIELD_ID,
        label = stringProvider.getString(SampleStringId.LOGIN_USERNAME_LABEL),
        text = "",
    ),
    ButtonListModel(
        label = stringProvider.getString(SampleStringId.LOGIN_SUBMIT),
        clickAction = SageAction.Noop,
        dataId = BUTTON_ID,
    ),
    FormMessageListModel(
        text = stringProvider.getString(SampleStringId.LOGIN_REJECTED),
        tone = FormMessageTone.ERROR,
        dataId = ERROR_MESSAGE_ID,
    ),
    FormMessageListModel(
        text = stringProvider.getString(SampleStringId.LOGIN_DEMO_ACCOUNT_HINT),
        tone = FormMessageTone.INFO,
        dataId = INFO_MESSAGE_ID,
    ),
)

internal const val TEXT_FIELD_ID = 2_000_001L
internal const val BUTTON_ID = 2_000_002L
internal const val ERROR_MESSAGE_ID = 2_000_003L
internal const val INFO_MESSAGE_ID = 2_000_004L
