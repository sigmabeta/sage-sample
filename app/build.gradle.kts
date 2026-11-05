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
        versionName = "0.1.0"
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
    implementation(projects.core.viewmodel)
    implementation(projects.features.browse)
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
    implementation(libs.sage.common.analytics)
    implementation(libs.sage.common.logging)
    implementation(libs.sage.common.coroutines)
    implementation(libs.sage.common.ui.components)
    implementation(libs.sage.common.ui.strings)
    implementation(libs.sage.common.ui.listScreens)

    implementation(libs.sage.android.coroutines)
    implementation(libs.sage.android.logging)
    implementation(libs.sage.android.ui.strings)
    implementation(libs.sage.android.ui.themes)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    // Voyager owns the navigation back stack; SAGE's catalog pins the version.
    implementation(libs.voyager.navigator)
    implementation(libs.voyager.transitions)

    implementation(libs.kotlinx.collections.immutable)
}
