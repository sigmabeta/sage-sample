plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // The `:benchmark` module is an Android Test module (`com.android.test`). Declared here so it
    // resolves against the same AGP version as the app.
    alias(libs.plugins.android.test) apply false
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

// One version for both apps: Android's versionName and the desktop build info both read it.
version = "0.1.0"

// Use the system Node + Yarn (Node 22 and Yarn 1.x are required) instead of letting Kotlin's JS
// plugin download its own. The download paths add project-level nodejs.org / yarnpkg repositories,
// which settings.gradle.kts's FAIL_ON_PROJECT_REPOS rejects before any JS task can run.
// Kotlin 2.3 exposes the switch on the EnvSpec types; the root extension still drives each
// project's convention (the deprecated field is the only global switch, hence the suppression).
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download.set(false)
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    @Suppress("DEPRECATION_ERROR")
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download = false
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().download.set(false)
    @Suppress("DEPRECATION_ERROR")
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().download = false
}
