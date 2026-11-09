package net.sigmabeta.sage.sample.listitems.screenshot

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.sample.models.SampleItem
import net.sigmabeta.sage.sample.ui.previews.fake.FakeModelGenerator

/**
 * One deterministic catalog shared by the three List Items screenshots.
 *
 * The feature's own `demoSampleItems()` is internal, so the fixture comes from the shared
 * [FakeModelGenerator]. The artwork seeds are derived from the ids by each state, so these rows
 * still render the same generated images the app would.
 */
internal object ListItemsFixtures {
    val items: ImmutableList<SampleItem> =
        FakeModelGenerator().randomSampleItems().toImmutableList()

    val loaded: LCE<List<SampleItem>> = LCE.Content(items)

    val empty: LCE<List<SampleItem>> = LCE.Content(emptyList())

    val loading: LCE<List<SampleItem>> = LCE.Loading(OPERATION_NAME)

    val error: LCE<List<SampleItem>> = LCE.Error(
        OPERATION_NAME,
        IllegalStateException("Screenshot fixture"),
    )

    /** The feature's operation name; private there, repeated here for the fixture's LCE payloads. */
    private const val OPERATION_NAME = "list-items"
}
