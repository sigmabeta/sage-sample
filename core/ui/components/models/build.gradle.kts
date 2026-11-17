plugins {
    // Compose-free on purpose: the features build these rows and must not depend on Compose. The
    // renderers in `:core:ui:components:api` re-export this module.
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.ui.components.models"
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                // `ListModel` and `SageAction`, the vocabulary these rows extend.
                api(libs.sage.common.ui.components)
                api(libs.sage.common.appcomm)
            }
        }
    }
}
