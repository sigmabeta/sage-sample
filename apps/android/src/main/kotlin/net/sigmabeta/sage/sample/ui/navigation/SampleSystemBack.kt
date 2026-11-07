package net.sigmabeta.sage.sample.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * The platform's whole-window Back input.
 *
 * Android's system Back is dispatched here. It is enabled only while the active tab cannot pop:
 * Voyager's inner `Navigator` handles a pushed screen itself, and at a tab root this handler is the
 * one that receives the request, so the shell's shared Back policy can still decide (Search root
 * returns Home, Home/Browse roots reach `onOuterBack`).
 */
@Composable
fun SampleSystemBack(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
