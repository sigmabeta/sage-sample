package net.sigmabeta.sage.sample.ui.theme.api

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * The sample's single Compose theme.
 *
 * [darkTheme] comes from the user's [net.sigmabeta.sage.sample.settings.ThemeMode] resolved against
 * the system setting (or directly from the device config in previews). [fontFamily] is supplied by
 * the platform app because Android loads the bundled Inter weights from `R.font` and desktop loads
 * them from the classpath; the shared type scale is [sampleTypography].
 *
 * Both apps and the screenshot harness use this, so a `MaterialTheme` cannot drift between them.
 */
@Composable
fun SampleTheme(
    darkTheme: Boolean,
    fontFamily: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        typography = sampleTypography(fontFamily),
        content = content,
    )
}
