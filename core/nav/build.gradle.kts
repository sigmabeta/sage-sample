plugins {
    // Multiplatform: the navigation policy is pure Kotlin, and the browser app needs the same
    // route codec the Android and desktop apps use.
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.nav"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                api(projects.core.models.api)
                api(projects.core.strings.api)

                api(libs.sage.common.nav)
                api(libs.sage.common.appcomm)
                api(libs.sage.common.list)
                api(libs.sage.common.ui.components)
                api(libs.sage.common.ui.iconsApi)
                // `foregroundActionFor` maps Lifecycle.Event, a multiplatform lifecycle type.
                api(libs.androidx.lifecycle.common)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
            }
        }
    }
}
