package net.sigmabeta.sage.sample.perfbench

import org.junit.Assert.assertEquals
import org.junit.Test

class FingerprintTest {
    @Test
    fun `builds a device fingerprint from probed facts`() {
        val fingerprint = deviceFingerprint(
            DeviceEnvironment(
                manufacturer = "Google",
                model = "Pixel 8",
                androidBuild = "AP1A.240505.004",
                abi = "arm64-v8a",
                thermalStatus = "none",
                appVersion = "0.1.0",
            ),
        )

        assertEquals("Google", fingerprint.manufacturer)
        assertEquals("Pixel 8", fingerprint.model)
        assertEquals("AP1A.240505.004", fingerprint.androidBuild)
        assertEquals("arm64-v8a", fingerprint.abi)
        assertEquals("none", fingerprint.thermalStatus)
        assertEquals("0.1.0", fingerprint.appVersion)
    }
}
