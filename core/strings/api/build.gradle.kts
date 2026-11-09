plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.strings.api"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                // SageStringId — the only type the id vocabulary needs.
                api(libs.sage.common.ui.strings)
            }
        }
    }
}
