plugins {
    // Multiplatform because the shell is consumed by both the Android app and the desktop app.
    // Sources live in `src/main/java` (the convention's `jvmSharedMain`), because the screen
    // mapping and the app graph-adjacent types come from pure-JVM modules.
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.compose.kmp)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.ui.shell"
    }

    sourceSets {
        named("jvmSharedMain") {
            dependencies {
                // Re-exported: both apps get the destinations, screens, view models and rendering
                // entry points from this one dependency.
                api(projects.core.nav)
                api(projects.core.models.api)
                api(projects.core.strings.api)
                api(projects.core.viewmodel)
                api(projects.core.settings)
                api(projects.core.ui.components.api)
                api(projects.core.ui.theme.api)
                // Serves the generated-bitmap preview harness that now lives here.
                api(projects.core.bitmaps)

                // The shell is the one module that maps destinations to feature view models.
                api(projects.features.browse)
                api(projects.features.contentdetail)
                api(projects.features.home)
                api(projects.features.listitems)
                api(projects.features.search)
                api(projects.features.settings)

                implementation(libs.sage.common.list)
                implementation(libs.sage.common.appcomm)
                implementation(libs.sage.common.ui.components)
                implementation(libs.sage.common.ui.iconsReal)
                implementation(libs.sage.common.ui.listScreens)

                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(libs.voyager.navigator)
                implementation(libs.voyager.tab.navigator)
                implementation(libs.voyager.transitions)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.coil.kt.compose)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
            }
        }
    }
}

dependencies {
    // The generated-bitmap preview harness uses the @Preview annotation, which each target
    // publishes separately (androidx for Android, JetBrains Compose for desktop).
    add("androidMainImplementation", platform(libs.androidx.compose.bom))
    add("androidMainCompileOnly", libs.androidx.compose.ui.tooling.preview)
    add(
        "jvmMainCompileOnly",
        "org.jetbrains.compose.ui:ui-tooling-preview:" +
            libs.versions.composeMultiplatform.get(),
    )
}
