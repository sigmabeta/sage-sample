package net.sigmabeta.sage.sample.ui.components.api.previews

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import net.sigmabeta.sage.sample.ui.components.api.ComponentStringId
import net.sigmabeta.sage.sample.ui.components.api.LocalComponentStringProvider
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

/**
 * Preview wrapper for the component renderers.
 *
 * Mirrors the app composition roots closely enough for a meaningful render: a Material 3 theme in
 * light or dark, plus the [LocalComponentStringProvider] the renderers read accessibility labels
 * from. The preview provider is intentionally English-only; the apps map the same ids to
 * `R.string` on Android and to `SampleStrings` on desktop.
 */
@Composable
fun ComponentsPreview(
    forceDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (forceDark) darkColorScheme() else lightColorScheme(),
        typography = Typography(),
    ) {
        CompositionLocalProvider(LocalComponentStringProvider provides PreviewStringProvider) {
            content()
        }
    }
}

internal object PreviewStringProvider : StringProvider {
    private val strings: Map<SageStringId, String> = mapOf(
        ComponentStringId.ACCY_OCL_CHECKBOX to "Toggle checkbox",
        ComponentStringId.ACCY_OCL_DROPDOWN to "Expand dropdown",
        ComponentStringId.ACCY_OCL_RATING to "Rate",
        ComponentStringId.ACCY_OCL_SINGLE_LINE to "Open %s",
        ComponentStringId.ACCY_OCL_VALUE to "Open",
        ComponentStringId.ACCY_ST_DESC_CHECKED to "Checked",
        ComponentStringId.ACCY_ST_DESC_LOADING to "Loading",
        ComponentStringId.ACCY_ST_DESC_UNCHECKED to "Unchecked",
    )

    override fun getString(string: SageStringId): String =
        strings[string] ?: string.toString()

    override fun getStringOneArg(string: SageStringId, arg: String): String =
        getString(string).format(arg)

    override fun getStringOneInt(string: SageStringId, arg: Int): String =
        getString(string).format(arg)

    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        getString(string).format(first, second)
}
