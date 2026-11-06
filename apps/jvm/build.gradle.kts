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

// The generated resource is regenerated for every build so its timestamp is the build's, not a
// stale cached one. It feeds JvmBuildInfo -> AppInfo -> the Settings About rows.
val appVersion = rootProject.version.toString()
val generatedBuildInfoDir = layout.buildDirectory.dir("generated/build-info")
val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outputDir = generatedBuildInfoDir
    val versionName = appVersion
    outputs.dir(outputDir)
    outputs.upToDateWhen { false }
    doLast {
        val file = outputDir.get().file("sample-build-info.properties").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "versionName=$versionName\nbuildTimeMs=${System.currentTimeMillis()}\n",
        )
    }
}

dependencies {
    implementation(projects.core.bitmaps)
    implementation(projects.core.settings)
    implementation(projects.core.viewmodel)
    implementation(projects.features.browse)
    implementation(projects.features.listitems)
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
    implementation(libs.sage.common.appinfo)
    implementation(libs.sage.common.analytics)
    implementation(libs.sage.common.logging)
    implementation(libs.sage.common.coroutines)
    implementation(libs.sage.common.ui.components)
    implementation(libs.sage.common.ui.iconsReal)
    implementation(libs.sage.common.ui.strings)
    implementation(libs.sage.common.ui.listScreens)
    implementation(libs.sage.common.storage.common)

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

    // Coil serves `generated://` requests through :core:bitmaps' fetcher; compose provides
    // AsyncImage for the preview.
    implementation(libs.coil.kt.meta)
    implementation(libs.coil.kt.compose)

    implementation(libs.voyager.navigator)
    implementation(libs.voyager.screenmodel)
    implementation(libs.voyager.transitions)

    testImplementation(libs.junit4)
}

sourceSets.main {
    resources.srcDir(generatedBuildInfoDir)
}

tasks.named("processResources") {
    dependsOn(generateBuildInfo)
}

compose.desktop {
    application {
        mainClass = "net.sigmabeta.sage.sample.jvm.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "sage-sample"
            packageVersion = appVersion
            // Bundle the full JDK runtime: the app pulls in java.desktop (Compose Desktop uses
            // AWT/Swing) and module inference can miss it, surfacing only as a runtime error.
            includeAllModules = true
        }
    }
}
