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
        // True only for the `benchmark` build type: MainActivity then reports the app fully drawn
        // when its first image cell composes, which the startup macrobenchmark records.
        buildConfigField("boolean", "BENCHMARK", "false")
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
        }
        // The variant the `:benchmark` test APK instruments. Release-shaped and profileable so
        // numbers describe production code, but debug-signed so it installs anywhere.
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            isProfileable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "BENCHMARK", "true")
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
    // The Compose/Voyager shell, and through it the shared core modules and every feature.
    implementation(projects.core.ui.shell)
    // Binds the in-process demo repositories for this app's graph; the browser binds the
    // HTTP-backed implementations instead.
    implementation(projects.core.repository.demo)

    // Android platform seams used directly by this module.
    implementation(projects.core.bitmaps)
    implementation(projects.core.strings.real)
    implementation(projects.core.settings)
    implementation(projects.core.ui.components.api)

    // The AppScope marker (net.sigmabeta.sage.di.AppScope) used by the app graph.
    implementation(libs.sage.common.di)

    // Metro's ViewModel map (`ViewModelGraph`) and the factory installed above Compose.
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)

    implementation(libs.sage.common.appinfo)
    implementation(libs.sage.common.analytics)
    implementation(libs.sage.common.logging)
    implementation(libs.sage.common.coroutines)
    implementation(libs.sage.common.ui.perfCompose)
    implementation(libs.sage.common.ui.strings)
    implementation(libs.sage.common.storage.common)

    implementation(libs.sage.android.coroutines)
    implementation(libs.sage.android.logging)
    implementation(libs.sage.android.ui.strings)

    // Preferences DataStore backs the app's SAGE Storage implementation (settings, theme).
    implementation(libs.androidx.dataStore.preferences)

    implementation(libs.androidx.activity.compose)

    // `SingletonImageLoader` registers :core:bitmaps' `generated://` fetcher at startup.
    implementation(libs.coil.kt.meta)
}
