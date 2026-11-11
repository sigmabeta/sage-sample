plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.sage.compose.kmp)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.ui.components.api"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                // Re-exported: consumers get SAGE's ListModel vocabulary and this module's
                // renderers from one dependency.
                // `ArtworkCorner`, the corner a destination's artwork arrives from: part of the
                // route payload the renderers read through `LocalArtworkHandoff`.
                implementation(projects.core.models.api)

                api(libs.sage.common.ui.components)
                api(libs.sage.common.ui.strings)
                api(projects.core.strings.api)

                implementation(libs.sage.common.appcomm)
                implementation(libs.sage.common.images)
                implementation(libs.sage.common.ui.perfCompose)
                implementation(libs.sage.common.ui.iconsReal)

                // `FakeImage` renders the shared preview art instead of a Coil placeholder.
                implementation(projects.core.bitmaps)

                implementation(libs.coil.kt.core)
                implementation(libs.coil.kt.compose)
                // `SingletonImageLoader`, the loader the apps install: the renderers read its
                // memory cache to draw a handoff bitmap on the frame a screen first appears.
                implementation(libs.coil.kt.meta)
                // `platformImageModel` hands Coil an okio Path for raw filesystem paths.
                implementation(libs.okio)
            }
        }
    }
}

dependencies {
    // The Android actual of `TraceSection`: Perfetto / systrace sections the device scroll
    // macrobenchmark sums. Android-only; the JVM and JS actuals are no-ops.
    add("androidMainImplementation", libs.androidx.tracing.ktx)

    // @Preview harnesses for the component renderers. Declared through the top-level handler so
    // the AndroidX BOM uses Gradle's `platform()`, not the KMP source-set DSL's deprecated one.
    add("androidMainImplementation", platform(libs.androidx.compose.bom))
    add("androidMainImplementation", libs.androidx.compose.ui.tooling.preview)
    add("androidMainImplementation", projects.core.strings.real)
    add("androidMainImplementation", libs.androidx.compose.ui.tooling)
}
