package net.sigmabeta.sage.sample.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import net.sigmabeta.sage.sample.R
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.ui.navigation.SampleNavHost
import net.sigmabeta.sage.sample.ui.theme.api.SampleTheme

/**
 * Composition root. The theme wraps the navigation host; everything below it is a destination.
 *
 * [settingsManager] is app-scoped and persisted, so a theme or font chosen on the Settings screen
 * recomposes this root and applies immediately. Screens resolve their own view models with
 * `metroViewModel()`, so this does not carry any feature state across the navigation boundary.
 *
 * [onOuterBack] is what the shell does when Back reaches past the tab roots; the activity supplies
 * `finish()`.
 */
@Composable
fun SampleApp(settingsManager: SampleSettingsManager, onOuterBack: () -> Unit = {}) {
    val themeMode by settingsManager.themeMode.collectAsState()
    val fontChoice by settingsManager.fontChoice.collectAsState()
    val fontFamily = remember(fontChoice) { fontFamilyFor(fontChoice) }

    SampleTheme(
        darkTheme = themeMode.isDark(isSystemInDarkTheme()),
        fontFamily = fontFamily,
    ) {
        SampleNavHost(onOuterBack = onOuterBack)
    }
}

/**
 * The Compose family for [choice].
 *
 * Inter ships as three OTF weights in `res/font`; the family covers the weights the Material type
 * scale uses so bold and medium text stay true to the typeface instead of being synthesized.
 */
private fun fontFamilyFor(choice: FontChoice): FontFamily = when (choice) {
    FontChoice.STANDARD -> FontFamily.Default

    FontChoice.INTER -> FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_bold, FontWeight.Bold),
    )
}
