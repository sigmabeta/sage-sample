package net.sigmabeta.sage.sample.ui.previews

import android.util.DisplayMetrics
import app.cash.paparazzi.DeviceConfig
import com.android.resources.Density
import com.android.resources.NightMode
import net.sigmabeta.sage.list.WidthClass
import kotlin.math.round

/**
 * The Paparazzi device matrix and its width-class mapping, shared by every screenshot module.
 *
 * Each entry is a parameterized-test quad: `[DeviceConfig, category, width, height]`. The matrix is
 * every interesting device in both light and dark, so one `@Test` per screen state produces one
 * golden per device × theme.
 */
object PreviewTestUtils {
    const val SUFFIX_TESTNAME = "{1} {2}x{3}"

    private const val QUAD_INDEX_SCREEN_HEIGHT = 3

    private val INTERESTING_DEVICES_INTERNAL = listOf(
        "Small Tablet" to DeviceConfig.NEXUS_7,
        "Beeg Tablet" to DeviceConfig.NEXUS_10,
        "Square Tablet" to DeviceConfig.NEXUS_10.copy(
            screenHeight = 768,
            screenWidth = 1024,
            density = Density.MEDIUM,
        ),
        "Small Phone" to DeviceConfig.NEXUS_4,
        "Mid Phone" to DeviceConfig.PIXEL_6,
        "Beeg Phone" to DeviceConfig.PIXEL_6_PRO,
    ).map { pair ->
        val category = pair.first
        val config = pair.second
        arrayOf(config, category, config.screenWidth.toString(), config.screenHeight.toString())
    }

    val INTERESTING_DEVICES: List<Array<Any>> = INTERESTING_DEVICES_INTERNAL.map { deviceConfigQuad ->
        val originalConfigName = deviceConfigQuad[1] as String
        val originalConfig = deviceConfigQuad[0] as DeviceConfig

        val lightConfig = originalConfig.copy(nightMode = NightMode.NOTNIGHT)

        arrayOf(
            lightConfig,
            "$originalConfigName (Light)",
            deviceConfigQuad[2],
            deviceConfigQuad[QUAD_INDEX_SCREEN_HEIGHT],
        )
    } + INTERESTING_DEVICES_INTERNAL.map { deviceConfigQuad ->
        val originalConfigName = deviceConfigQuad[1] as String
        val originalConfig = deviceConfigQuad[0] as DeviceConfig

        val darkConfig = originalConfig.copy(nightMode = NightMode.NIGHT)

        arrayOf(
            darkConfig,
            "$originalConfigName (Dark)",
            deviceConfigQuad[2],
            deviceConfigQuad[QUAD_INDEX_SCREEN_HEIGHT],
        )
    }
}

/** Maps a Paparazzi device width onto SAGE's 600 dp / 840 dp breakpoints. */
fun DeviceConfig.toWidthClass(): WidthClass {
    val density = this.density.dpiValue
    val scalingFactor = density.toFloat() / DisplayMetrics.DENSITY_DEFAULT
    val relevantWidthDp = round(this.screenWidth / scalingFactor)

    return when {
        relevantWidthDp < WIDTH_DP_COMPACT_MAX -> WidthClass.COMPACT
        relevantWidthDp < WIDTH_DP_MEDIUM_MAX -> WidthClass.MEDIUM
        else -> WidthClass.EXPANDED
    }
}

private const val WIDTH_DP_COMPACT_MAX = 600
private const val WIDTH_DP_MEDIUM_MAX = 840
