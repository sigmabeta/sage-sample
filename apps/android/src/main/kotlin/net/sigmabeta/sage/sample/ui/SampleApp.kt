package net.sigmabeta.sage.sample.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.ui.navigation.SampleNavHost

/**
 * Composition root. The theme wraps the navigation host; everything below it is a destination.
 *
 * [settingsManager] is app-scoped and persisted, so a theme chosen on the Settings screen
 * recomposes this root and applies immediately. Screens resolve their own view models with
 * `metroViewModel()`, so this does not carry any feature state across the navigation boundary.
 */
@Composable
fun SampleApp(settingsManager: SampleSettingsManager) {
    val themeMode by settingsManager.themeMode.collectAsState()

    MaterialTheme(
        colorScheme = if (themeMode.isDark(isSystemInDarkTheme())) {
            darkColorScheme()
        } else {
            lightColorScheme()
        },
        typography = Typography(),
    ) {
        SampleNavHost()
    }
}
