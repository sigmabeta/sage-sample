package net.sigmabeta.sage.sample.ui.shell

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.ui.theme.api.SampleTheme

/**
 * Composition root shared by both apps. The theme wraps the navigation host; everything below it is
 * a destination.
 *
 * [settingsManager] is app-scoped and persisted, so a theme or font chosen on the Settings screen
 * recomposes this root and applies immediately. [interFontFamily] is the platform's bundled Inter
 * family (Android `res/font`, desktop classpath resources), or the default family when the app has
 * no bundled weights.
 *
 * Platform seams keep this class platform-neutral:
 * - [platformBack] supplies Back input and any window-level gesture.
 * - [screenContainer] supplies a per-screen ViewModel store where the platform's navigator does
 *   not (desktop).
 * - [screenLifecycleOwner] is the foreground signal (activity lifecycle, or window focus on
 *   desktop); `null` falls back to the composition's `LocalLifecycleOwner`.
 *
 * [onOuterBack] is what the shell does when Back reaches past the tab roots; Android supplies
 * `finish()` and desktop leaves it empty.
 *
 * [browserNavigation] is the browser's history bridge (null on Android/desktop). The shell reports
 * its active route to it and lets it handle Back when an app entry exists behind the current URL.
 */
@Composable
fun SampleApp(
    settingsManager: SampleSettingsManager,
    interFontFamily: FontFamily = FontFamily.Default,
    platformBack: SamplePlatformBack = NoopSamplePlatformBack,
    screenContainer: SampleScreenContainer = PassthroughSampleScreenContainer,
    screenLifecycleOwner: LifecycleOwner? = null,
    browserNavigation: SampleBrowserNavigation? = null,
    onOuterBack: () -> Unit = {},
) {
    val themeMode by settingsManager.themeMode.collectAsState()
    val fontChoice by settingsManager.fontChoice.collectAsState()
    val fontFamily = remember(fontChoice, interFontFamily) {
        fontFamilyFor(fontChoice, interFontFamily)
    }
    val lifecycleOwner = screenLifecycleOwner ?: LocalLifecycleOwner.current

    CompositionLocalProvider(
        LocalSamplePlatformBack provides platformBack,
        LocalSampleScreenContainer provides screenContainer,
        LocalSampleBrowserNavigation provides browserNavigation,
        LocalScreenLifecycleOwner provides lifecycleOwner,
    ) {
        SampleTheme(
            darkTheme = themeMode.isDark(isSystemInDarkTheme()),
            fontFamily = fontFamily,
        ) {
            SampleNavHost(onOuterBack = onOuterBack)
        }
    }
}

/** The Compose family for [choice]; Inter is the platform-provided bundled family. */
private fun fontFamilyFor(choice: FontChoice, interFontFamily: FontFamily): FontFamily = when (choice) {
    FontChoice.STANDARD -> FontFamily.Default
    FontChoice.INTER -> interFontFamily
}
