plugins {
    // Pure JVM: the settings vocabulary and its Storage-backed manager only use SAGE's common
    // modules, so both the Android and desktop apps can share one implementation.
    alias(libs.plugins.sage.jvm)
    // Metro compiler plugin, so the manager's @Inject constructor and its contributed bindings
    // are processed at compile time.
    alias(libs.plugins.sage.di)
}

dependencies {
    api(libs.sage.common.storage.common)
    // SAGE's debug-delay seam; the sample's manager is a duration-aware extension of it.
    api(libs.sage.common.list)
    api(libs.sage.common.di)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
}
