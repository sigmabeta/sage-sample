package net.sigmabeta.sage.sample.login.screenshot

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import net.sigmabeta.sage.sample.ui.previews.PreviewTestUtils.INTERESTING_DEVICES
import net.sigmabeta.sage.sample.ui.previews.PreviewTestUtils.SUFFIX_TESTNAME
import net.sigmabeta.sage.sample.ui.previews.toWidthClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LoginScreenshots(
    private val deviceConfig: DeviceConfig,
    @Suppress("UNUSED_PARAMETER") private val category: String,
    @Suppress("UNUSED_PARAMETER") private val width: String,
    @Suppress("UNUSED_PARAMETER") private val height: String,
) {
    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun loginEmpty() = snapshot { LoginEmpty(syntheticWidthClass = deviceConfig.toWidthClass()) }

    @Test
    fun loginFilled() = snapshot { LoginFilled(syntheticWidthClass = deviceConfig.toWidthClass()) }

    @Test
    fun loginSubmitting() = snapshot { LoginSubmitting(syntheticWidthClass = deviceConfig.toWidthClass()) }

    @Test
    fun loginRejected() = snapshot { LoginRejected(syntheticWidthClass = deviceConfig.toWidthClass()) }

    @Test
    fun loginThrottled() = snapshot { LoginThrottled(syntheticWidthClass = deviceConfig.toWidthClass()) }

    @Test
    fun loginUnreachable() = snapshot { LoginUnreachable(syntheticWidthClass = deviceConfig.toWidthClass()) }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.unsafeUpdateConfig(deviceConfig = deviceConfig)
        paparazzi.snapshot { content() }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = SUFFIX_TESTNAME)
        fun getDeviceConfig(): Iterable<Array<Any>> = INTERESTING_DEVICES
    }
}
