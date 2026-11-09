import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "net.sigmabeta.sage.sample.buildlogic"

// Build-logic runs inside the Gradle daemon (JDK 21); targeting 21 lets the convention classpath
// consume Gradle plugins published for Java 21 (Paparazzi 2.0.0-alpha). Module targets stay JVM 17
// through the SAGE base plugins this convention layers on.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    // The SAGE base plugins are applied by id at runtime, and `namespaceFromPath()` is called from
    // the screenshot convention, so the SAGE convention classes and the `libs` helper ride along.
    implementation("net.sigmabeta.sage.buildlogic:convention")
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    // implementation, not compileOnly: `app.cash.paparazzi` is applied by id at runtime, so its
    // classes must be on the runtime classpath of this build.
    implementation(libs.paparazzi)
}

gradlePlugin {
    plugins {
        register("sageSampleScreenshot") {
            id = "sagesample.screenshot"
            implementationClass = "SageSampleScreenshotPlugin"
        }
    }
}
