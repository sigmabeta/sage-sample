plugins {
    // Multiplatform: the settings vocabulary and its Storage-backed manager are shared by
    // Android, desktop and the browser client.
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    // Metro compiler plugin, so the manager's @Inject constructor and its contributed bindings
    // are processed at compile time.
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.settings"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(libs.sage.common.di)
                api(libs.sage.common.storage.common)
                // SAGE's debug-delay seam; the sample's manager is a duration-aware extension of it.
                api(libs.sage.common.list)
                api(libs.sage.common.di)

                implementation(libs.kotlinx.coroutines.core)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
            }
        }
    }
}
