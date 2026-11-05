plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    // The desktop app (:apps:jvm) applies the JetBrains Compose plugin directly; declaring it here
    // pins its version once for the build.
    alias(libs.plugins.compose.multiplatform) apply false
    // The SAGE base plugins (sage.android / sage.jvm / sage.kmp) apply detekt and ktlint by id.
    // Declaring them here `apply false` puts their runtime on the shared build classpath.
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}
