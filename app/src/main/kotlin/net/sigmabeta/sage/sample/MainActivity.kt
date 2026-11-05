package net.sigmabeta.sage.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.sample.ui.SampleApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No @AndroidEntryPoint member injection: entry points read the graph off the
        // application and pull the bindings they need.
        val brain = (application as SampleApplication).appGraph.welcomeBrain

        if (savedInstanceState == null) {
            brain.sendAction(SageAction.InitNoArgs)
        }

        setContent { SampleApp(brain) }
    }
}
