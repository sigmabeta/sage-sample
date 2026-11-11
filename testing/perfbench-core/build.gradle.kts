plugins {
    // Multiplatform so the Android benchmark APK and the JVM diff CLI share one result schema.
    // It has no JS target: nothing in the browser measures performance.
    alias(libs.plugins.sage.kmp)
    // The result document is the contract, so its DTOs are serializable.
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.perfbench"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                // Re-exported: every consumer needs the generated serializers to read the file.
                api(libs.kotlinx.serialization.json)
                // SAGE's KMP convention opts every module into coroutines' experimental API; the
                // artifact keeps that opt-in marker resolvable on this module's compile classpath.
                api(libs.kotlinx.coroutines.core)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
            }
        }
    }
}
