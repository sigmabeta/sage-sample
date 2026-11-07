package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import net.sigmabeta.sage.sample.jvm.SampleStrings
import net.sigmabeta.sage.sample.ui.components.api.CrossfadeText
import net.sigmabeta.sage.ui.Icon as SageIcon
import net.sigmabeta.sage.ui.vector

/**
 * The shared top bar, rendering the state derived by [topBarState].
 *
 * The title belongs to the active screen's SAGE title state; the leading affordance is a
 * hamburger that opens Settings ([SampleTopBarLeading.MENU]) or a back arrow that pops the screen
 * ([SampleTopBarLeading.BACK]). Navigation-tab labels stay app-owned in [SampleDestination].
 *
 * [scrollBehavior] is the shell's `enterAlways` behavior: the bar collapses as the content
 * scrolls and is re-expanded by a navigation reset. The title stays one line, crossfades when it
 * changes, and marquees when it is wider than the bar rather than clipping abruptly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleTopBar(
    state: SampleTopBarState,
    scrollBehavior: TopAppBarScrollBehavior,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            CrossfadeText(
                text = state.title,
                textAlign = TextAlign.Center,
                maxLines = 1,
                textModifier = Modifier.basicMarquee(),
                contentAlignment = Alignment.Center,
            )
        },
        navigationIcon = {
            when (state.leading) {
                SampleTopBarLeading.MENU -> IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = SageIcon.Menu.vector(),
                        contentDescription = SampleStrings.CD_OPEN_SETTINGS,
                    )
                }

                SampleTopBarLeading.BACK -> IconButton(onClick = onBack) {
                    Icon(
                        imageVector = SageIcon.Back.vector(),
                        contentDescription = SampleStrings.CD_NAVIGATE_BACK,
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
