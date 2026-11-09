package net.sigmabeta.sage.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import net.sigmabeta.sage.sample.ui.AndroidPlatformBack
import net.sigmabeta.sage.sample.ui.components.api.LocalSampleStringProvider
import net.sigmabeta.sage.sample.ui.shell.SampleApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No @AndroidEntryPoint member injection: entry points read the graph off the
        // application. Here that is only the ViewModel factory — screens resolve their own
        // view models from it with metroViewModel().
        val graph = (application as SampleApplication).appGraph

        setContent {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
                LocalSampleStringProvider provides graph.stringProvider,
            ) {
                // Back at a tab root leaves the tabbed shell: finish the activity, which is the
                // platform's default Back behaviour.
                SampleApp(
                    settingsManager = graph.settingsManager,
                    interFontFamily = InterFontFamily,
                    platformBack = AndroidPlatformBack,
                    onOuterBack = { finish() },
                )
            }
        }
    }
}

/**
 * Inter ships as three OTF weights in `res/font`; the family covers the weights the Material type
 * scale uses so bold and medium text stay true to the typeface instead of being synthesized.
 */
private val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold),
)
