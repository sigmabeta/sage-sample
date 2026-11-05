plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    // Metro is SAGE's DI compiler plugin. Every module that declares or consumes an
    // `@Inject` constructor / `@ContributesTo(AppScope::class)` binding applies it.
    alias(libs.plugins.metro)
    alias(libs.plugins.detekt)
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

dependencies {
    implementation(projects.features.welcome)

    // The AppScope marker (net.sigmabeta.sage.di.AppScope) used by the app graph.
    implementation(libs.sage.common.di)

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

    implementation(libs.kotlinx.collections.immutable)
}
