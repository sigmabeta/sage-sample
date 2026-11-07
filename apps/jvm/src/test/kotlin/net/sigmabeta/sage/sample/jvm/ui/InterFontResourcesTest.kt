package net.sigmabeta.sage.sample.jvm.ui

import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Guards the desktop font paths used by [SampleApp]'s Inter family.
 *
 * Compose Desktop resolves `Font("fonts/…")` through the class loader at render time, so a
 * missing resource would only fail once the user picks Inter. This catches a packaging or path
 * typo in the JVM app's unit tests instead.
 */
class InterFontResourcesTest {
    @Test
    fun `inter font files are packaged on the classpath`() {
        listOf(
            "fonts/inter_regular.otf",
            "fonts/inter_medium.otf",
            "fonts/inter_bold.otf",
        ).forEach { path ->
            assertNotNull("Missing bundled font $path", javaClass.classLoader.getResource(path))
        }
    }
}
