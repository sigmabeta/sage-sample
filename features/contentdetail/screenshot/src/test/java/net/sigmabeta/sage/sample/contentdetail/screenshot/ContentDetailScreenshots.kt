package net.sigmabeta.sage.sample.contentdetail.screenshot

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
class ContentDetailScreenshots(
    private val deviceConfig: DeviceConfig,
    @Suppress("UNUSED_PARAMETER") private val category: String,
    @Suppress("UNUSED_PARAMETER") private val width: String,
    @Suppress("UNUSED_PARAMETER") private val height: String,
) {
    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun contentDetailLoaded() = snapshot {
        ContentDetailLoaded(syntheticWidthClass = deviceConfig.toWidthClass())
    }

    @Test
    fun contentDetailLoading() = snapshot {
        ContentDetailLoading(syntheticWidthClass = deviceConfig.toWidthClass())
    }

    @Test
    fun contentDetailNotFound() = snapshot {
        ContentDetailNotFound(syntheticWidthClass = deviceConfig.toWidthClass())
    }

    @Test
    fun contentDetailError() = snapshot {
        ContentDetailError(syntheticWidthClass = deviceConfig.toWidthClass())
    }

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
