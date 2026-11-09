plugins {
    // Multiplatform: the domain models are consumed by the Android/desktop apps, the shared
    // features and renderers, and the browser client. All three targets compile the same data
    // classes, so there is no platform-specific model code.
    alias(libs.plugins.sage.kmp)
    // Opt-in JS target, gated behind -Psage.js (see SAGE's SageKmpJsPlugin).
    alias(libs.plugins.sage.kmp.js)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.models"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                // `ContentAction` carries a SAGE `Icon`; the models describe renderer input.
                api(libs.sage.common.ui.iconsApi)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
            }
        }
    }
}
