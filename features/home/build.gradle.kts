plugins {
    // A feature module is pure JVM: its view model depends only on SAGE's common (multiplatform)
    // modules and the sample's ViewModel base, so it stays free of Android types.
    alias(libs.plugins.sage.jvm)
    // Metro compiler plugin, so the view model's `@Inject` constructor, the module set, and its
    // contribution into the ViewModel map are all processed at compile time.
    alias(libs.plugins.sage.di)
}

dependencies {
    api(projects.core.viewmodel)
    // The Content detail route codec: rows open detail through the one shared encoding.
    implementation(projects.features.contentdetail)
    // `@ViewModelKey`, the map key Metro uses to bind ViewModels.
    api(libs.metrox.viewmodel)

    api(libs.sage.common.list)
    api(libs.sage.common.appcomm)
    api(libs.sage.common.analytics)
    api(libs.sage.common.logging)
    api(libs.sage.common.ui.components)
    api(libs.sage.common.ui.strings)

    // ImmutableList backs each module's section items, which HomeState wraps for rendering.
    implementation(libs.kotlinx.collections.immutable)

    // `GeneratedImageFetcher.urlFor` is the single source of truth for `generated://` request
    // URLs. The feature only builds the URL; the apps' Coil loaders do the rendering.
    implementation(projects.core.bitmaps)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
