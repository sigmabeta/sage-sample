import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    // Same pure-JVM base as the feature modules: Kotlin/JVM, JVM 17, detekt and ktlint.
    alias(libs.plugins.sage.jvm)
    // Metro: this module owns the desktop graph and aggregates every feature's ViewModel.
    alias(libs.plugins.sage.di)
    // Compose Multiplatform desktop. The Kotlin Compose compiler handles @Composable codegen;
    // the JetBrains plugin supplies `compose.desktop.currentOs` and `compose.desktop.application`.
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

dependencies {
    implementation(projects.core.viewmodel)
    implementation(projects.features.browse)
    implementation(projects.features.search)
    implementation(projects.features.settings)
    implementation(projects.features.welcome)

    // Metro's ViewModel map (`ViewModelGraph`) and the Compose resolver (`metroViewModel()`).
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)
    // ViewModel + the per-screen `LocalViewModelStoreOwner` on desktop (Voyager only supplies
    // one on Android, so the app wraps each screen itself — see PerScreenViewModelStore.kt).
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewModelCompose)

    implementation(libs.sage.common.list)
    implementation(libs.sage.common.appcomm)
    implementation(libs.sage.common.analytics)
    implementation(libs.sage.common.logging)
    implementation(libs.sage.common.coroutines)
    implementation(libs.sage.common.ui.components)
    implementation(libs.sage.common.ui.iconsReal)
    implementation(libs.sage.common.ui.strings)
    implementation(libs.sage.common.ui.listScreens)

    // Platform-agnostic Compose from the catalog; the per-OS Skia native comes from
    // `compose.desktop.currentOs` above.
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.ui)
    implementation(compose.desktop.currentOs)

    implementation(libs.kotlinx.coroutines.core)
    // Dispatchers.Main on the desktop target is the AWT event queue. ViewModels launch onto it,
    // so without this artifact `viewModelScope` fails with "Main dispatcher is missing".
    implementation(libs.kotlinx.coroutines.swing)

    implementation(libs.voyager.navigator)
    implementation(libs.voyager.screenmodel)
    implementation(libs.voyager.transitions)

    testImplementation(libs.junit4)
}

compose.desktop {
    application {
        mainClass = "net.sigmabeta.sage.sample.jvm.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "sage-sample"
            packageVersion = "1.0.0"
            // Bundle the full JDK runtime: the app pulls in java.desktop (Compose Desktop uses
            // AWT/Swing) and module inference can miss it, surfacing only as a runtime error.
            includeAllModules = true
        }
    }
}
