package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.sample.jvm.SampleStrings
import net.sigmabeta.sage.ui.Icon as SageIcon
import net.sigmabeta.sage.ui.vector

/**
 * The shared top bar, rendering the state derived by [topBarState].
 *
 * The title belongs to the active screen's SAGE title state; the leading affordance is a
 * hamburger that opens Settings ([SampleTopBarLeading.MENU]) or a back arrow that pops the screen
 * ([SampleTopBarLeading.BACK]). Navigation-tab labels stay app-owned in [SampleDestination].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleTopBar(
    state: SampleTopBarState,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(state.title) },
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
    )
}
