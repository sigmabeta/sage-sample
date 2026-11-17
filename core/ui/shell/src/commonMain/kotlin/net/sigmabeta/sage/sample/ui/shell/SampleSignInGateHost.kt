package net.sigmabeta.sage.sample.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.sample.nav.SampleBackAction
import net.sigmabeta.sage.sample.nav.SampleGateSurface
import net.sigmabeta.sage.sample.nav.gateBackActionFor
import net.sigmabeta.sage.sample.nav.topBarState
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.ui.components.api.text

/**
 * The Sign-in gate above the tabbed shell.
 *
 * The surface comes from [SampleSignInGateHolder]: a blank surface while the startup check runs,
 * Login while Signed out, and the tabbed shell while Signed in. The shell is keyed on the User and
 * leaves composition whenever the client is not Signed in, so a Sign out (or a Session lost to a
 * 401) disposes every tab's inner navigator together with the screens and view models in it, and
 * the next Sign in opens fresh tab stacks.
 *
 * Back on Blank or Login falls through to the host ([gateBackActionFor]), like Back at Home's root.
 */
@Composable
internal fun SampleSignInGateHost(identityRepository: IdentityRepository, onOuterBack: () -> Unit) {
    val holder = rememberSaveable(
        identityRepository,
        saver = SampleSignInGateHolder.saver(identityRepository),
    ) {
        SampleSignInGateHolder(identityRepository)
    }
    val deepLinks = LocalSampleDeepLinkSource.current
    LaunchedEffect(holder, deepLinks) {
        holder.follow(deepLinks)
    }

    val outerBack by rememberUpdatedState(onOuterBack)
    val gate by holder.gate.collectAsState()

    when (val surface = gate.surface) {
        is SampleGateSurface.Shell -> key(surface.user.id) {
            SampleNavHost(signInGate = holder, onOuterBack = onOuterBack)
        }

        SampleGateSurface.Blank -> SampleBlankSurface(onBack = { gatedBack(surface, outerBack) })

        SampleGateSurface.Login -> SampleLoginSurface(onBack = { gatedBack(surface, outerBack) })
    }
}

private fun gatedBack(surface: SampleGateSurface, onOuterBack: () -> Unit) {
    if (gateBackActionFor(surface) == SampleBackAction.Outer) onOuterBack()
}

/** Shown while the startup check runs: the theme's background and nothing else. */
@Composable
private fun SampleBlankSurface(onBack: () -> Unit) {
    val platformBack = LocalSamplePlatformBack.current
    SampleSystemBack(enabled = true, onBack = onBack)
    Box(
        modifier = platformBack.wrap(Modifier.fillMaxSize(), onBack)
            .background(MaterialTheme.colorScheme.background),
    )
}

/**
 * Login on its own: a title bar with no hamburger or back arrow, and no navigation bar or rail.
 *
 * [LoginScreen] gets its own root Voyager [Navigator], so its view model lives in a per-screen
 * store (wrapped by [LocalSampleScreenContainer] where the platform needs it) that is cleared when
 * Login leaves composition. Login's one-shot events are not navigation: the gate follows the
 * identity seam instead, so they are dropped here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleLoginSurface(onBack: () -> Unit) {
    var titleBar by remember { mutableStateOf<TitleBarModel?>(null) }
    val titleBarSink = remember { SampleTitleBarSink { reported -> titleBar = reported } }
    val navEffects = remember { SampleNavEffects { } }
    val platformBack = LocalSamplePlatformBack.current
    val title = topBarState(
        titleBar = titleBar,
        destinationLabel = SampleStringId.LOGIN_TITLE.text(),
        isTopLevel = true,
    ).title

    CompositionLocalProvider(
        LocalSampleNavEffects provides navEffects,
        LocalSampleTitleBarSink provides titleBarSink,
        LocalSampleWidthClass provides currentWidthClass(),
    ) {
        SampleSystemBack(enabled = true, onBack = onBack)

        Scaffold(
            modifier = platformBack.wrap(Modifier.fillMaxSize(), onBack),
            topBar = { CenterAlignedTopAppBar(title = { Text(title) }) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                Navigator(screen = LoginScreen, onBackPressed = null) { navigator ->
                    val screens = LocalSampleScreenContainer.current
                    val screen = navigator.lastItem
                    navigator.saveableState("login", screen) {
                        screens.Content(screen) { screen.Content() }
                    }
                }
            }
        }
    }
}
