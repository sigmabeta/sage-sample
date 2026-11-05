plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    // The SAGE base plugins (sage.android / sage.jvm / sage.kmp) apply detekt and ktlint by id.
    // Declaring them here `apply false` puts their runtime on the shared build classpath.
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}
