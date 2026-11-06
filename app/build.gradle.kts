plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    // Metro is SAGE's DI compiler plugin. Every module that declares or consumes an
    // `@Inject` constructor / `@ContributesTo(AppScope::class)` binding applies it.
    alias(libs.plugins.metro)
    alias(libs.plugins.detekt)
    // The app applies plugins directly (SAGE's convention plugins target library modules), so it
    // opts into ktlint itself — otherwise `./gradlew ktlintCheck` would skip this module.
    alias(libs.plugins.ktlint)
}

android {
    namespace = "net.sigmabeta.sage.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.sigmabeta.sage.sample"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = rootProject.version.toString()

        // Settings' About section displays these. The timestamp is captured when the build's
        // BuildConfig is generated, so it reflects the build rather than the install.
        buildConfigField("long", "BUILD_TIME_MS", "${System.currentTimeMillis()}L")
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // BuildConfig.VERSION_NAME / VERSION_CODE / BUILD_TIME_MS feed the Settings About rows.
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

detekt {
    config.setFrom("$rootDir/detekt-config.yml")
    baseline = file("detekt-baseline.xml")
}

ktlint {
    // Pin the engine to SAGE's catalog version so the app formats identically to modules that go
    // through the sage.jvm / sage.android conventions (which call configureKtlint()).
    version.set(libs.versions.ktlintTool.get())
}

dependencies {
    implementation(projects.core.bitmaps)
    implementation(projects.core.settings)
    implementation(projects.core.ui.components.api)
    implementation(projects.core.viewmodel)
    implementation(projects.features.browse)
    implementation(projects.features.listitems)
    implementation(projects.features.search)
    implementation(projects.features.settings)
    implementation(projects.features.welcome)

    // The AppScope marker (net.sigmabeta.sage.di.AppScope) used by the app graph.
    implementation(libs.sage.common.di)

    // Metro's ViewModel map (`ViewModelGraph`) and the Compose resolver (`metroViewModel()`).
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)

    // SAGE's navigation vocabulary (`RouteDescriptor`, `ArgType`) for the app's destinations.
    implementation(libs.sage.common.nav)
    // SAGE's icon set; `Icon.vector()` turns it into a Compose ImageVector.
    implementation(libs.sage.common.ui.iconsReal)

    implementation(libs.sage.common.list)
    implementation(libs.sage.common.appcomm)
    implementation(libs.sage.common.appinfo)
    implementation(libs.sage.common.analytics)
    implementation(libs.sage.common.logging)
    implementation(libs.sage.common.coroutines)
    implementation(libs.sage.common.ui.components)
    implementation(libs.sage.common.ui.perfCompose)
    implementation(libs.sage.common.ui.strings)
    implementation(libs.sage.common.ui.listScreens)
    implementation(libs.sage.common.storage.common)

    implementation(libs.sage.android.coroutines)
    implementation(libs.sage.android.logging)
    implementation(libs.sage.android.ui.strings)

    // Preferences DataStore backs the app's SAGE Storage implementation (settings, theme).
    implementation(libs.androidx.dataStore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    // `@Preview` for the generated-bitmap preview; the tooling runtime is debug-only.
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    // Coil serves `generated://` requests through :core:bitmaps' fetcher; compose provides
    // AsyncImage for the preview.
    implementation(libs.coil.kt.meta)
    implementation(libs.coil.kt.compose)

    // Voyager owns the navigation back stack; SAGE's catalog pins the version.
    implementation(libs.voyager.navigator)
    implementation(libs.voyager.transitions)

    implementation(libs.kotlinx.collections.immutable)

    // Unit tests for app-owned logic that needs no Android runtime (e.g. route mapping).
    testImplementation(libs.junit4)
}
