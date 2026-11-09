plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.sage.compose.kmp)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.ui.theme.api"
    }
}
