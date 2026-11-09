plugins {
    // Multiplatform: the same feature view model runs on Android, desktop and the browser.
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    // Metro compiler plugin, so the view model's `@Inject` constructor and its contribution into
    // the ViewModel map are processed at compile time.
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.feature.browse"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(libs.sage.common.di)
                // The feature reads through the repository seam; each app decides whether
                // the implementation is the in-process demo source or the HTTP client.
                api(projects.core.repository.api)
                implementation(projects.core.strings.api)
                api(projects.core.models.api)
                api(projects.core.viewmodel)
                // `@ViewModelKey`, the map key Metro uses to bind ViewModels.
                api(libs.metrox.viewmodel)

                api(libs.sage.common.list)
                api(libs.sage.common.appcomm)
                api(libs.sage.common.analytics)
                api(libs.sage.common.logging)
                api(libs.sage.common.ui.components)
                api(libs.sage.common.ui.strings)
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
