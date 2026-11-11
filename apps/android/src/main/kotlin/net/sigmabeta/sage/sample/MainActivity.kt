package net.sigmabeta.sage.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import java.util.concurrent.atomic.AtomicBoolean
import net.sigmabeta.sage.sample.nav.SampleDeepLink
import net.sigmabeta.sage.sample.ui.AndroidPlatformBack
import net.sigmabeta.sage.sample.ui.components.api.LocalSampleStringProvider
import net.sigmabeta.sage.sample.ui.components.api.subs.LocalFirstContentReporter
import net.sigmabeta.sage.sample.ui.shell.MutableSampleDeepLinkSource
import net.sigmabeta.sage.sample.ui.shell.SampleApp

class MainActivity : ComponentActivity() {
    /**
     * This Activity's one-shot deep-link source.
     *
     * Per Activity rather than app-scoped: a `FLAG_ACTIVITY_CLEAR_TASK` relaunch creates the new
     * Activity while the outgoing one's shell may still be collecting. A shared source lets the
     * outgoing shell consume the new launch's route, leaving the new shell on Home.
     */
    private val deepLinks = MutableSampleDeepLinkSource()

    // In benchmark builds, report the app fully drawn the first time a content-bearing image
    // composes — the "first content" the device startup macrobenchmark measures
    // (StartupTimingMetric's `timeToFullDisplayMs`). A fresh guard per Activity instance, so every
    // warm relaunch reports again. Outside the benchmark build the reporter is null and costs
    // nothing.
    private val firstContentReporter: (() -> Unit)? = if (BuildConfig.BENCHMARK) {
        val reported = AtomicBoolean(false)
        val report: () -> Unit = { if (reported.compareAndSet(false, true)) reportFullyDrawn() }
        report
    } else {
        null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No @AndroidEntryPoint member injection: entry points read the graph off the
        // application. Here that is only the ViewModel factory — screens resolve their own
        // view models from it with metroViewModel().
        val graph = (application as SampleApplication).appGraph

        // Only a fresh launch applies the intent's link. A recreated activity (rotation, process
        // restore) keeps the shell state it saved instead of replaying the link on top of it.
        if (savedInstanceState == null) {
            offerDeepLink(intent)
        }

        setContent {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
                LocalSampleStringProvider provides graph.stringProvider,
                LocalFirstContentReporter provides firstContentReporter,
            ) {
                // Back at a tab root leaves the tabbed shell: finish the activity, which is the
                // platform's default Back behaviour.
                SampleApp(
                    settingsManager = graph.settingsManager,
                    interFontFamily = InterFontFamily,
                    platformBack = AndroidPlatformBack,
                    deepLinkSource = deepLinks,
                    onOuterBack = { finish() },
                )
            }
        }
    }

    /**
     * `launchMode="singleTop"` makes a link tapped while this activity is on top arrive here
     * instead of stacking a second shell; the intent's URI is decoded by the shared route codec.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        offerDeepLink(intent)
    }

    /** Offers the intent's deep link to the shell; a plain launcher intent has none. */
    private fun offerDeepLink(intent: Intent?) {
        deepLinks.offer(intent?.dataString?.let(SampleDeepLink::routeFor))
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
