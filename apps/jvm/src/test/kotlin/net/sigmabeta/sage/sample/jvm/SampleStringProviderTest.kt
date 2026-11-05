package net.sigmabeta.sage.sample.jvm

import net.sigmabeta.sage.sample.feature.browse.BrowseStringId
import net.sigmabeta.sage.sample.feature.search.SearchStringId
import net.sigmabeta.sage.sample.feature.settings.SettingsStringId
import net.sigmabeta.sage.sample.feature.welcome.WelcomeStringId
import net.sigmabeta.sage.ui.SageStringId
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Every feature string id must resolve on the desktop target.
 *
 * Features own closed-set `SageStringId` enums; the desktop app is one of the two places that
 * must map all of them (the other is Android's `SampleBindings`). This fails when a feature adds
 * an id without updating [SampleStringProvider], rather than at runtime when the screen renders.
 */
class SampleStringProviderTest {
    @Test
    fun `every feature string id has a desktop mapping`() {
        val ids: List<SageStringId> = WelcomeStringId.entries +
            BrowseStringId.entries +
            SearchStringId.entries +
            SettingsStringId.entries

        ids.forEach { id ->
            assertFalse("Missing desktop text for $id", SampleStringProvider.getString(id).isBlank())
        }
    }
}
