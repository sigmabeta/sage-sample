package net.sigmabeta.sage.sample.ui

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.sample.ui.navigation.SampleNavHost
import net.sigmabeta.sage.ui.themes.SageMaterial

/**
 * Composition root. The theme wraps the navigation host; everything below it is a destination.
 *
 * Screens resolve their own view models with `metroViewModel()`, so this does not carry any
 * feature state across the navigation boundary.
 */
@Composable
fun SampleApp() {
    SageMaterial(
        lightColors = lightColorScheme(),
        darkColors = darkColorScheme(),
        typography = Typography(),
    ) {
        SampleNavHost()
    }
}
