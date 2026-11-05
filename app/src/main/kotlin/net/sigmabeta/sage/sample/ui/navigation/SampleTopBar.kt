package net.sigmabeta.sage.sample.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.sigmabeta.sage.sample.R
import net.sigmabeta.sage.ui.Icon as SageIcon
import net.sigmabeta.sage.ui.vector

/**
 * The shared top bar.
 *
 * The leading icon is the navigation affordance for the *current* destination: a hamburger on
 * top-level screens (opens Settings) and a back arrow everywhere else (pops the screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleTopBar(
    destination: SampleDestination,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(destination.labelRes)) },
        navigationIcon = {
            if (destination.isTopLevel) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = SageIcon.Menu.vector(),
                        contentDescription = stringResource(R.string.cd_open_settings),
                    )
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = SageIcon.Back.vector(),
                        contentDescription = stringResource(R.string.cd_navigate_back),
                    )
                }
            }
        },
    )
}
