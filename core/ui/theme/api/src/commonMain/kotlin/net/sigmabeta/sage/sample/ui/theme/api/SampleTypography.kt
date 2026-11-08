package net.sigmabeta.sage.sample.ui.theme.api

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

/**
 * The sample's Material 3 type scale, rendered in [fontFamily].
 *
 * Both apps render the same screens, so the type scale is shared; each app only maps the user's
 * persisted [net.sigmabeta.sage.sample.settings.FontChoice] to a platform [FontFamily] (Android
 * loads the bundled font as a resource, desktop from the classpath) and passes it here.
 *
 * [FontFamily.Default] reproduces the Material 3 defaults, which is what the "Standard" choice
 * uses.
 */
@Suppress("LongMethod")
fun sampleTypography(fontFamily: FontFamily = FontFamily.Default): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}
