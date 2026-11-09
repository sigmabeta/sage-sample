plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.feature.contentdetail"
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
                api(libs.metrox.viewmodel)

                api(libs.sage.common.list)
                api(libs.sage.common.appcomm)
                api(libs.sage.common.analytics)
                api(libs.sage.common.logging)
                api(libs.sage.common.ui.components)
                // `StringGenerator` (the seeded fake-data generator) ships with SAGE's string module.
                api(libs.sage.common.ui.strings)

                // `GeneratedImageFetcher.urlFor` is the single source of truth for `generated://`
                // request URLs. The feature only builds the URL; the apps' loaders do the rendering.
                implementation(projects.core.bitmaps)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
                implementation(libs.kotlinx.coroutines.test)
                // The demo repository/generator tests live beside the feature they exercise.
                implementation(projects.core.repository.demo)
            }
        }
    }
}
