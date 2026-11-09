import com.android.build.api.dsl.LibraryExtension
import net.sigmabeta.sage.plugins.components.namespaceFromPath
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.withType

/**
 * Convention for the per-feature screenshot modules (`:features:<x>:screenshot`).
 *
 * Each module renders its screen's SAGE [net.sigmabeta.sage.list.ListState] through the real list
 * pipeline with hand-built fixtures and Paparazzi-snapshots it across the shared device matrix. A
 * module only declares its own feature dependency — this convention contributes the namespace, the
 * shared preview harness, the SAGE list/appcomm libraries, and the renderer suite every preview
 * needs.
 *
 * Disables the JUnit test reports: Paparazzi 2.0.0-alpha05's HTML reporter calls
 * `TestResultsProvider.hasOutput(...)`, which Gradle 9.x removed; snapshots still record/verify in
 * the test JVM, only the post-run HTML summary crashes the task. Remove the report opt-out once
 * Paparazzi ships a Gradle-9 compatible build.
 */
class SageSampleScreenshotPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("sage.android")
                apply("sage.compose.android")
                apply("app.cash.paparazzi")
            }

            extensions.configure<LibraryExtension> {
                // Derives `net.sigmabeta.sage.sample.<feature>.screenshot` from the module path.
                namespace = namespaceFromPath()
            }

            dependencies {
                add("implementation", "net.sigmabeta.sage:appcomm")
                add("implementation", "net.sigmabeta.sage:list")
                add("implementation", project(":core:ui:previews"))
                // Fixtures resolve any literal text (section titles, field labels) through the
                // shared provider rather than an app-side map.
                add("implementation", project(":core:strings:real"))
            }

            tasks.withType<Test>().configureEach {
                reports.html.required.set(false)
                reports.junitXml.required.set(false)
            }
        }
    }
}
