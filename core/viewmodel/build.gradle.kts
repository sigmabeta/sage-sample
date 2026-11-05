plugins {
    // Pure JVM: the base class depends only on androidx.lifecycle's multiplatform ViewModel plus
    // SAGE's common modules, so every feature module can extend it without Android types.
    alias(libs.plugins.sage.jvm)
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel)
    // The base exposes the settings-backed SampleDelayManager every feature view model supplies.
    api(projects.core.settings)

    api(libs.sage.common.list)
    api(libs.sage.common.appcomm)
    api(libs.sage.common.analytics)
    api(libs.sage.common.coroutines)
    api(libs.sage.common.ui.strings)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    // `ListState` returns `ListModel`, and SAGE's list module keeps `ui:components` internal.
    testImplementation(libs.sage.common.ui.components)
}
