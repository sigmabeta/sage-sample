package net.sigmabeta.sage.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import net.sigmabeta.sage.appcomm.VglsAction
import net.sigmabeta.sage.sample.feature.welcome.WelcomeViewModelBrain
import net.sigmabeta.sage.sample.ui.SampleApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var brain: WelcomeViewModelBrain

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            brain.sendAction(VglsAction.InitNoArgs)
        }

        setContent { SampleApp(brain) }
    }
}
