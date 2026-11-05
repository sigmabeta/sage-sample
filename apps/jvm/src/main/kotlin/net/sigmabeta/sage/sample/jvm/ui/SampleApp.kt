package net.sigmabeta.sage.sample.jvm.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.sample.jvm.ui.navigation.SampleNavHost

/**
 * Composition root. The theme wraps the navigation host; everything below it is a destination.
 *
 * Android gets `SageMaterial` from `sage.android.ui.themes`, which is Android-only. The desktop
 * app inlines the same light/dark `MaterialTheme` wrapper, following the system theme.
 */
@Composable
fun SampleApp() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        typography = Typography(),
    ) {
        SampleNavHost()
    }
}
