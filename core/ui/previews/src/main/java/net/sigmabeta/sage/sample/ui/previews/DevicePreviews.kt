package net.sigmabeta.sage.sample.ui.previews

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-`@Preview` annotation for the screenshot fixtures: light/dark × phone/foldable/tablet.
 *
 * The device specs match [PreviewTestUtils.INTERESTING_DEVICES] closely enough that the IDE gutter
 * and the Paparazzi goldens show the same layouts (the exact pixel sizes differ).
 */
@Preview(name = "Light | Phone | Portrait", device = "spec:width=360dp,height=640dp,dpi=480")
@Preview(name = "Light | Phone | Landscape", device = "spec:width=640dp,height=360dp,dpi=480")
@Preview(name = "Light | Foldable", device = "spec:width=673dp,height=841dp,dpi=480")
@Preview(name = "Light | Tablet", device = "spec:width=1280dp,height=800dp,dpi=480")
@Preview(name = "Dark | Phone | Portrait", device = "spec:width=360dp,height=640dp,dpi=480", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Dark | Phone | Landscape", device = "spec:width=640dp,height=360dp,dpi=480", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Dark | Foldable", device = "spec:width=673dp,height=841dp,dpi=480", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Dark | Tablet", device = "spec:width=1280dp,height=800dp,dpi=480", uiMode = UI_MODE_NIGHT_YES)
annotation class DevicePreviews
