plugins {
    alias(libs.plugins.sage.jvm)
}

dependencies {
    api(projects.core.models.api)
    api(projects.core.strings.api)

    api(libs.sage.common.nav)
    api(libs.sage.common.appcomm)
    api(libs.sage.common.list)
    api(libs.sage.common.ui.components)
    api(libs.sage.common.ui.iconsApi)
    // `foregroundActionFor` maps Lifecycle.Event, a JVM-friendly lifecycle-common type.
    api(libs.androidx.lifecycle.common)

    testImplementation(libs.junit4)
}
