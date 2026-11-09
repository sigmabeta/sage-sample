package net.sigmabeta.sage.sample.listitems.screenshot

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
class GridOfImagesScreenshots(
    private val deviceConfig: DeviceConfig,
    @Suppress("UNUSED_PARAMETER") private val category: String,
    @Suppress("UNUSED_PARAMETER") private val width: String,
    @Suppress("UNUSED_PARAMETER") private val height: String,
) {
    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun gridOfImages() = snapshot { GridOfImages(syntheticWidthClass = deviceConfig.toWidthClass()) }

    @Test
    fun gridOfImagesLoading() = snapshot {
        GridOfImagesLoading(syntheticWidthClass = deviceConfig.toWidthClass())
    }

    @Test
    fun gridOfImagesEmpty() = snapshot {
        GridOfImagesEmpty(syntheticWidthClass = deviceConfig.toWidthClass())
    }

    @Test
    fun gridOfImagesError() = snapshot {
        GridOfImagesError(syntheticWidthClass = deviceConfig.toWidthClass())
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
