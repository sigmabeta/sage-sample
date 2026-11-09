plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.feature.settings"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(libs.sage.common.di)
                // The feature reads through the repository seam; each app decides whether
                // the implementation is the in-process demo source or the HTTP client.
                api(projects.core.repository.api)
                implementation(projects.core.strings.api)
                api(projects.core.viewmodel)
                // The persisted preferences the screen edits; also exposes the delay seam.
                api(projects.core.settings)
                api(libs.metrox.viewmodel)

                api(libs.sage.common.list)
                api(libs.sage.common.appcomm)
                api(libs.sage.common.appinfo)
                api(libs.sage.common.analytics)
                api(libs.sage.common.logging)
                api(libs.sage.common.ui.components)
                api(libs.sage.common.ui.strings)

                implementation(libs.kotlinx.collections.immutable)
                // Localized build-date formatting used to come from java.time; the shared date
                // range now uses kotlinx-datetime so the browser compiles it too.
                implementation(libs.kotlinx.datetime)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
