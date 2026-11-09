plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.strings.real"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                api(projects.core.strings.api)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
            }
        }
    }
}
