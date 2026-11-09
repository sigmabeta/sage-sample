plugins {
    // Multiplatform: the repository contracts and the wire models are shared by the apps, the
    // demo data source (JVM) and the browser's HTTP client (JS).
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    // The JSON wire DTOs are @Serializable, so the server and the browser client share one shape.
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.repository"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                api(projects.core.models.api)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.json)
                // Repositories stream SAGE LCE values; consumers of this module see that type.
                api(libs.sage.common.appcomm)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
            }
        }
    }
}
