plugins {
    // A feature module is pure JVM: its brain depends only on SAGE's common (multiplatform)
    // modules, so it stays free of Android types and is unit-testable without a device.
    alias(libs.plugins.sage.jvm)
    // Metro compiler plugin, so the brain's `@Inject` constructor is available to the app graph.
    alias(libs.plugins.sage.di)
}

dependencies {
    api(libs.sage.common.list)
    api(libs.sage.common.appcomm)
    api(libs.sage.common.analytics)
    api(libs.sage.common.logging)
    api(libs.sage.common.ui.components)
    api(libs.sage.common.ui.strings)
}
