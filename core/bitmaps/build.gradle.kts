plugins {
    // Multiplatform because the generator is shared by the Android app and the desktop app:
    // commonMain compiles once against Compose Multiplatform, and each consumer resolves the
    // variant for its own target.
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.compose.kmp)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.bitmaps"
    }

    sourceSets {
        named("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
