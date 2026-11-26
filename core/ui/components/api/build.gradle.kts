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
                // Re-exported: consumers get SAGE's ListModel vocabulary, string/action seams and the
                // shared renderer suite (`ListModel.Content`, `SageRenderers`, shared-element locals)
                // from one dependency.
                api(libs.sage.common.ui.composables)
                // The sample-owned rows (form fields, buttons, messages) these renderers draw.
                api(projects.core.ui.components.models)
                api(projects.core.strings.api)

                // `SearchField` resolves its icon vectors through the real icon implementation.
                implementation(libs.sage.common.ui.iconsReal)

                // `SampleRenderers.previewImage` renders the shared preview art instead of a Coil
                // placeholder, keeping the Paparazzi goldens byte-identical.
                implementation(projects.core.bitmaps)
            }
        }
    }
}

dependencies {
    // @Preview harnesses for the sample's own form rows. Declared through the top-level handler so
    // the AndroidX BOM uses Gradle's `platform()`, not the KMP source-set DSL's deprecated one.
    add("androidMainImplementation", platform(libs.androidx.compose.bom))
    add("androidMainImplementation", libs.androidx.compose.ui.tooling.preview)
    add("androidMainImplementation", projects.core.strings.real)
    add("androidMainImplementation", libs.androidx.compose.ui.tooling)
}
