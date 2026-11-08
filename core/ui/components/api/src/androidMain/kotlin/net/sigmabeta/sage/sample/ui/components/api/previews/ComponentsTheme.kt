package net.sigmabeta.sage.sample.ui.components.api.previews

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import net.sigmabeta.sage.sample.strings.real.SampleStringProvider
import net.sigmabeta.sage.sample.ui.components.api.LocalSampleStringProvider

/**
 * Preview wrapper for the component renderers.
 *
 * Mirrors the app composition roots closely enough for a meaningful render: a Material 3 theme in
 * light or dark, plus the [LocalSampleStringProvider] the renderers read accessibility labels
 * from. It installs the same shared [SampleStringProvider] the apps do, so a preview and a golden
 * resolve identical text.
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
        CompositionLocalProvider(LocalSampleStringProvider provides SampleStringProvider) {
            content()
        }
    }
}
