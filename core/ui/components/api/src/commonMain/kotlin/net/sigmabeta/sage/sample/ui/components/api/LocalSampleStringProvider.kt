package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.ui.StringProvider

/**
 * Composable access to the app's shared [StringProvider].
 *
 * Both composition roots install the graph's provider (backed by
 * `:core:strings:real`'s `SampleStringProvider`) just above the navigation host, so a renderer can
 * resolve an accessibility label without reaching into DI. The local is static because the
 * provider never changes for the lifetime of the tree.
 */
val LocalSampleStringProvider = staticCompositionLocalOf<StringProvider> {
    error(
        "LocalSampleStringProvider not provided. Wrap the Compose tree in " +
            "CompositionLocalProvider(LocalSampleStringProvider provides stringProvider) { ... }",
    )
}

@Composable
@ReadOnlyComposable
fun SampleStringId.text(): String = LocalSampleStringProvider.current.getString(this)

@Composable
@ReadOnlyComposable
fun SampleStringId.text(arg: String): String =
    LocalSampleStringProvider.current.getStringOneArg(this, arg)

@Composable
@ReadOnlyComposable
fun SampleStringId.textInt(arg: Int): String =
    LocalSampleStringProvider.current.getStringOneInt(this, arg)

@Composable
@ReadOnlyComposable
fun SampleStringId.text(first: String, second: String): String =
    LocalSampleStringProvider.current.getStringTwoArgs(this, first, second)
