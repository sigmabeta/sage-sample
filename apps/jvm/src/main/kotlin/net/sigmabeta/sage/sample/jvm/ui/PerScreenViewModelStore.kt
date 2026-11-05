package net.sigmabeta.sage.sample.jvm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen

/**
 * Gives every Voyager [Screen] its own [ViewModelStore] on the desktop target.
 *
 * Voyager's per-screen `ViewModelStoreOwner` plumbing is Android-only: on JVM/desktop its
 * lifecycle provider is empty, so every screen would share the Compose `Window`'s store and
 * `metroViewModel<VM>()` would return the same cached instance across pushes.
 *
 * The store hangs off a Voyager [ScreenModel] (via [rememberScreenModel], keyed by [screen]).
 * Voyager keeps screen models for as long as the screen stays in the back stack and disposes them
 * only when it is popped, so a view model survives being covered by another screen and is cleared
 * when its screen leaves the stack — matching the Android app, where popping and re-pushing builds
 * a fresh view model.
 */
@Composable
fun WithPerScreenViewModelStore(screen: Screen, content: @Composable () -> Unit) {
    val holder = screen.rememberScreenModel { ViewModelStoreHolder() }
    val owner = remember(holder) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = holder.store
        }
    }

    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}

/** Owns one screen's store; Voyager calls [onDispose] when the screen leaves the back stack. */
private class ViewModelStoreHolder : ScreenModel {
    val store = ViewModelStore()

    override fun onDispose() {
        store.clear()
    }
}
