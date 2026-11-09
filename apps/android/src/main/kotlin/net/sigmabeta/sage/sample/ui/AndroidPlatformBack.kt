package net.sigmabeta.sage.sample.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.sample.ui.shell.SamplePlatformBack

/**
 * Android's system Back: a lifecycle callback delivered through [BackHandler].
 *
 * It is enabled only while the active tab cannot pop. Voyager's inner `Navigator` handles a pushed
 * screen itself, and at a tab root this handler is the one that receives the request, so the
 * shell's shared Back policy can still decide (Search root returns Home, Home/Browse roots reach
 * `onOuterBack`).
 */
object AndroidPlatformBack : SamplePlatformBack {
    @Composable
    override fun Handler(enabled: Boolean, onBack: () -> Unit) {
        BackHandler(enabled = enabled, onBack = onBack)
    }
}
