package net.sigmabeta.sage.sample.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.sigmabeta.sage.ui.vector

/**
 * Material's medium-width breakpoint: below it the navigation suite is a bottom bar, at or above
 * it a rail. This mirrors Material 3's navigation-suite layout without taking a dependency on the
 * `material3-adaptive-navigation-suite` artifact (which pins material3 to an older version than
 * the Compose BOM this app builds against).
 */
private const val MEDIUM_WIDTH_BREAKPOINT_DP = 600

/**
 * The app's navigation chrome: shared top bar, plus the top-level destinations as a bottom bar on
 * compact windows and a rail on wider ones.
 *
 * The rail is a sibling of the [Scaffold] rather than part of it, so it spans the full window
 * height next to the content instead of sitting below the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleNavScaffold(
    destination: SampleDestination,
    onSelectDestination: (SampleDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val useRail = isWideWindow()

    Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
            NavigationRail(modifier = Modifier.safeDrawingPadding()) {
                SampleDestination.topLevel.forEach { item ->
                    NavigationRailItem(
                        selected = item == destination,
                        onClick = { onSelectDestination(item) },
                        icon = { DestinationIcon(item) },
                        label = { Text(stringResource(item.labelRes)) },
                    )
                }
            }
        }

        Scaffold(
            // Take the space left over beside the rail (or the whole window when there is none).
            modifier = Modifier.weight(1f).fillMaxHeight(),
            topBar = {
                SampleTopBar(
                    destination = destination,
                    onOpenSettings = onOpenSettings,
                    onBack = onBack,
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        SampleDestination.topLevel.forEach { item ->
                            NavigationBarItem(
                                selected = item == destination,
                                onClick = { onSelectDestination(item) },
                                icon = { DestinationIcon(item) },
                                label = { Text(stringResource(item.labelRes)) },
                            )
                        }
                    }
                }
            },
            content = content,
        )
    }
}

/** The item's label is always rendered next to the icon, so the icon can stay decorative. */
@Composable
private fun DestinationIcon(destination: SampleDestination) {
    Icon(
        imageVector = destination.icon.vector(),
        contentDescription = null,
    )
}

@Composable
private fun isWideWindow(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val width = with(LocalDensity.current) { windowInfo.containerSize.width.toDp() }
    return width >= MEDIUM_WIDTH_BREAKPOINT_DP.dp
}
