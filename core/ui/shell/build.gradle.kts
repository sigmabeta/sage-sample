plugins {
    // Multiplatform because the shell is consumed by the Android app, the desktop app and the
    // browser app. Sources live in `src/commonMain/kotlin`; only the desktop preview harness is
    // JVM-specific.
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.sage.compose.kmp)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.ui.shell"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                // Re-exported: every app gets the destinations, screens, view models and rendering
                // entry points from this one dependency.
                api(projects.core.nav)
                api(projects.core.models.api)
                api(projects.core.strings.api)
                api(projects.core.viewmodel)
                api(projects.core.settings)
                api(projects.core.ui.components.api)
                api(projects.core.ui.theme.api)
                // Serves the generated-bitmap preview harness that now lives here.
                api(projects.core.bitmaps)

                // The shell is the one module that maps destinations to feature view models.
                api(projects.features.browse)
                api(projects.features.contentdetail)
                api(projects.features.home)
                api(projects.features.listitems)
                api(projects.features.search)
                api(projects.features.settings)

                implementation(libs.sage.common.list)
                implementation(libs.sage.common.appcomm)
                implementation(libs.sage.common.ui.components)
                implementation(libs.sage.common.ui.iconsReal)
                implementation(libs.sage.common.ui.listScreens)

                // `LocalViewModelStoreOwner` (used by PerScreenViewModelStore) comes with
                // Metrox's Compose artifact, which resolves the JetBrains lifecycle variant on JS.
                // Don't add Google's lifecycle-viewmodel-compose here: it publishes no JS variant.
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(libs.voyager.navigator)
                implementation(libs.voyager.screenmodel)
                implementation(libs.voyager.tab.navigator)
                implementation(libs.voyager.transitions)

                implementation(libs.kotlinx.coroutines.core)
            }
        }

        named("jvmMain") {
            dependencies {
                // The desktop-only preview harness loads generated art through Coil.
                implementation(libs.coil.kt.compose)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
            }
        }
    }
}

dependencies {
    // The generated-bitmap preview harness uses the @Preview annotation, which each target
    // publishes separately (androidx for Android, JetBrains Compose for desktop).
    add("androidMainImplementation", platform(libs.androidx.compose.bom))
    add("androidMainCompileOnly", libs.androidx.compose.ui.tooling.preview)
    add(
        "jvmMainCompileOnly",
        "org.jetbrains.compose.ui:ui-tooling-preview:" +
            libs.versions.composeMultiplatform.get(),
    )
}
