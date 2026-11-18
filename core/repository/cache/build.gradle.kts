plugins {
    // Multiplatform: the in-process favorites (JVM, `:core:repository:demo`) and the browser's
    // HTTP favorites (JS, `:core:repository:remote`) share this cache.
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.repository.cache"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                api(projects.core.repository.api)
                implementation(libs.kotlinx.coroutines.core)
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
