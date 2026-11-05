plugins {
    // A feature module is pure JVM: its view model depends only on SAGE's common (multiplatform)
    // modules and the sample's ViewModel base, so it stays free of Android types.
    alias(libs.plugins.sage.jvm)
    // Metro compiler plugin, so the view model's `@Inject` constructor and its contribution into
    // the ViewModel map are processed at compile time.
    alias(libs.plugins.sage.di)
}

dependencies {
    api(projects.core.viewmodel)
    // `@ViewModelKey`, the map key Metro uses to bind ViewModels.
    api(libs.metrox.viewmodel)

    api(libs.sage.common.list)
    api(libs.sage.common.appcomm)
    api(libs.sage.common.analytics)
    api(libs.sage.common.logging)
    api(libs.sage.common.ui.components)
    api(libs.sage.common.ui.strings)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
