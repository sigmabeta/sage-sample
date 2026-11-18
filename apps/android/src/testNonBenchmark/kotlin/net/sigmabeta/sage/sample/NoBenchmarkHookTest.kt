package net.sigmabeta.sage.sample

import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Runs in the debug and release unit tests, against that build type's compiled app classes (the
 * same classes its APK ships; neither build type minifies). The benchmark launch hook signs a Demo
 * account in from an intent extra, so it must not exist outside the `benchmark` build type.
 */
class NoBenchmarkHookTest {
    @Test
    fun `this build type does not contain the benchmark sign-in hook`() {
        assertThrows(ClassNotFoundException::class.java) {
            Class.forName(BENCHMARK_SIGN_IN_CLASS)
        }
    }
}
