plugins {
    // Multiplatform: the browser client is a Kotlin/JS executable, and the same HTTP
    // implementation is exercised on the JVM against Ktor's MockEngine.
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.repository.remote"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(libs.sage.common.di)
                api(projects.core.repository.api)
                api(projects.core.settings)

                api(libs.ktor.client.core)
                api(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        // The JS target only exists when -Psage.js is set (see SAGE's SageKmpJsPlugin).
        if (providers.gradleProperty("sage.js").orNull.toBoolean()) {
            named("jsMain") {
                dependencies {
                    implementation(libs.ktor.client.js)
                }
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
    }
}
