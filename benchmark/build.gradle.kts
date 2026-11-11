plugins {
    alias(libs.plugins.android.test)
    // `com.android.test` is not one of SAGE's convention modules, so it opts into the shared
    // lint config itself; otherwise this module's Kotlin would never be checked.
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom("$rootDir/detekt-config.yml")
    baseline = file("detekt-baseline.xml")
}

ktlint {
    version.set(libs.versions.ktlintTool.get())
}

android {
    namespace = "net.sigmabeta.sage.sample.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Numbers are not representative on an emulator — remove this to run on physical hardware.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    targetProjectPath = ":apps:android"
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        // Match the app's benchmark variant by name.
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
    // The shared result schema, fingerprint, report math and diff contract.
    implementation(projects.testing.perfbenchCore)
    // `SampleDeepLink.SCHEME`, so the benchmark starts the app through the same scheme it registers
    // rather than a copy that can drift.
    implementation(projects.core.nav)
}
