plugins {
    // Pure JVM: the base class depends only on androidx.lifecycle's multiplatform ViewModel plus
    // SAGE's common modules, so every feature module can extend it without Android types.
    alias(libs.plugins.sage.jvm)
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel)

    api(libs.sage.common.list)
    api(libs.sage.common.appcomm)
    api(libs.sage.common.analytics)
    api(libs.sage.common.coroutines)
    api(libs.sage.common.ui.strings)
}
