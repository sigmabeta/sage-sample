package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

/**
 * Composable access to the app's [StringProvider].
 *
 * Both composition roots install their graph's provider just above the navigation host, so a
 * renderer can resolve an accessibility label without reaching into DI. The local is static
 * because the provider never changes for the lifetime of the tree.
 */
val LocalComponentStringProvider = staticCompositionLocalOf<StringProvider> {
    error(
        "LocalComponentStringProvider not provided. Wrap the Compose tree in " +
            "CompositionLocalProvider(LocalComponentStringProvider provides stringProvider) { ... }",
    )
}

@Composable
@ReadOnlyComposable
fun SageStringId.text(): String = LocalComponentStringProvider.current.getString(this)

@Composable
@ReadOnlyComposable
fun SageStringId.text(arg: String): String =
    LocalComponentStringProvider.current.getStringOneArg(this, arg)

@Composable
@ReadOnlyComposable
fun SageStringId.textInt(arg: Int): String =
    LocalComponentStringProvider.current.getStringOneInt(this, arg)

@Composable
@ReadOnlyComposable
fun SageStringId.text(first: String, second: String): String =
    LocalComponentStringProvider.current.getStringTwoArgs(this, first, second)
