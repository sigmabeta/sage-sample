package net.sigmabeta.sage.sample.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.sigmabeta.sage.sample.feature.welcome.WelcomeViewModelBrain
import net.sigmabeta.sage.ui.list.ListScreen
import net.sigmabeta.sage.ui.themes.SageMaterial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleApp(brain: WelcomeViewModelBrain) {
    val state by brain.uiStateActual.collectAsStateWithLifecycle()

    SageMaterial(
        lightColors = lightColorScheme(),
        darkColors = darkColorScheme(),
        typography = Typography(),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(state.title.title.orEmpty()) })
            },
        ) { padding ->
            ListScreen(
                state = state,
                actionSink = brain::sendAction,
                showDebug = false,
                sideMargin = 16.dp,
                modifier = Modifier.padding(padding),
                itemContent = { model, sink, _, mod, pad ->
                    ListItemContent(model, sink, mod, pad)
                },
            )
        }
    }
}
