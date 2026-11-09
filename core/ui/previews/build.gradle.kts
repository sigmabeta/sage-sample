plugins {
    alias(libs.plugins.sage.android)
    alias(libs.plugins.sage.compose.android)
}

android {
    namespace = "net.sigmabeta.sage.sample.ui.previews"
}

dependencies {
    // Re-exported: screenshot modules get the ListModel vocabulary and the shared renderers (plus
    // LocalSampleStringProvider) from one dependency.
    api(projects.core.ui.components.api)

    implementation(projects.core.models.api)
    implementation(projects.core.strings.real)
    implementation(projects.core.ui.theme.api)

    implementation(libs.sage.common.list)
    implementation(libs.sage.common.appcomm)
    implementation(libs.sage.common.ui.listScreens)
    implementation(libs.sage.common.ui.components)
    implementation(libs.sage.common.ui.iconsApi)
    implementation(libs.sage.common.ui.strings)

    // PreviewTestUtils references Paparazzi's DeviceConfig. Only leaf `:features:<x>:screenshot`
    // modules consume this module and nothing depends on them, so the Paparazzi runtime has no
    // path into the shipped apps — a plain implementation dependency is sufficient.
    implementation(libs.paparazzi.runtime)
}
