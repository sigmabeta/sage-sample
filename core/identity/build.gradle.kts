plugins {
    // Android + JVM only, like :core:database underneath it: Room has no JS target, so this module
    // does not apply sage.kmp.js and nothing in the browser app may depend on it.
    alias(libs.plugins.sage.kmp)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.identity"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                api(projects.core.database)
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
