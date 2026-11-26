package net.sigmabeta.sage.sample.ui.components.api.previews

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.sample.strings.real.SampleStringProvider
import net.sigmabeta.sage.sample.ui.components.api.ProvideSampleText

/**
 * Preview wrapper for the component renderers.
 *
 * Mirrors the app composition roots closely enough for a meaningful render: a Material 3 theme in
 * light or dark, plus the shared [ProvideSampleText] (the sample's `LocalSampleStringProvider` and
 * SAGE's renderer locals, all from the same [SampleStringProvider]). A preview and a golden resolve
 * identical text.
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
        ProvideSampleText(stringProvider = SampleStringProvider) {
            content()
        }
    }
}
