plugins {
    // Multiplatform: every feature view model extends this base, including the browser's.
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    // Metro compiler plugin: the ViewModel factory is contributed to AppScope here so every app
    // graph resolves one shared binding instead of each keeping its own copy.
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.viewmodel"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(libs.sage.common.di)
                api(libs.androidx.lifecycle.viewmodel)
                // `MetroViewModelFactory` is the binding's supertype, so consumers of this module
                // must see it.
                api(libs.metrox.viewmodel)
                // The base exposes the settings-backed SampleDelayManager every feature supplies.
                api(projects.core.settings)

                api(libs.sage.common.list)
                api(libs.sage.common.appcomm)
                api(libs.sage.common.analytics)
                api(libs.sage.common.coroutines)
                api(libs.sage.common.ui.strings)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
                implementation(libs.kotlinx.coroutines.test)
                // `ListState` returns `ListModel`, and SAGE's list module keeps `ui:components`
                // internal.
                implementation(libs.sage.common.ui.components)
            }
        }
    }
}
