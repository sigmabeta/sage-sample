package net.sigmabeta.sage.sample.ui.shell

import net.sigmabeta.sage.sample.settings.AnimationScale
import net.sigmabeta.sage.sample.ui.components.api.DEFAULT_ANIMATION_SCALE
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The renderers' fallback and the Settings default must describe the same motion.
 *
 * The renderer module cannot read the settings enum (the preference module is Compose-free), so the
 * shell — where both meet — is what keeps the two defaults from drifting.
 */
class AnimationScaleDefaultsTest {
    @Test
    fun `renderer fallback matches the settings default multiplier`() {
        assertEquals(AnimationScale.DEFAULT.multiplier, DEFAULT_ANIMATION_SCALE, 0f)
    }

    @Test
    fun `one times is the authored speed`() {
        assertEquals(1f, AnimationScale.ONE_X.multiplier, 0f)
    }
}
