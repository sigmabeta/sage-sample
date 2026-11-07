package net.sigmabeta.sage.sample.jvm.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.sample.jvm.ui.navigation.LocalPlatformBackKeys
import net.sigmabeta.sage.sample.jvm.ui.navigation.SampleNavHost
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.ui.components.api.sampleTypography

/**
 * Composition root. The theme wraps the navigation host; everything below it is a destination.
 *
 * [settingsManager] is app-scoped and persisted, so a theme or font chosen on the Settings screen
 * recomposes this root and applies immediately.
 *
 * [platformBackKeys] carries the window's Escape/Backspace signals into the shell, where they run
 * the same Back policy as the top-bar arrow. `null` (previews, tests) leaves them unwired.
 *
 * [onOuterBack] is the shell's bottom-of-stack Back branch; desktop leaves it empty.
 */
@Composable
fun SampleApp(
    settingsManager: SampleSettingsManager,
    platformBackKeys: Flow<Unit>? = null,
    onOuterBack: () -> Unit = {},
) {
    val themeMode by settingsManager.themeMode.collectAsState()
    val fontChoice by settingsManager.fontChoice.collectAsState()
    val fontFamily = remember(fontChoice) { fontFamilyFor(fontChoice) }

    CompositionLocalProvider(LocalPlatformBackKeys provides platformBackKeys) {
        MaterialTheme(
            colorScheme = if (themeMode.isDark(isSystemInDarkTheme())) {
                darkColorScheme()
            } else {
                lightColorScheme()
            },
            typography = sampleTypography(fontFamily),
        ) {
            SampleNavHost(onOuterBack = onOuterBack)
        }
    }
}

/**
 * The Compose family for [choice].
 *
 * Desktop loads the bundled OTF files from the classpath (`src/main/resources/fonts`) rather than
 * an Android `R.font` resource; the three weights otherwise match the Android mapping.
 */
private fun fontFamilyFor(choice: FontChoice): FontFamily = when (choice) {
    FontChoice.STANDARD -> FontFamily.Default

    FontChoice.INTER -> FontFamily(
        Font("fonts/inter_regular.otf", FontWeight.Normal),
        Font("fonts/inter_medium.otf", FontWeight.Medium),
        Font("fonts/inter_bold.otf", FontWeight.Bold),
    )
}
