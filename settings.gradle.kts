// Propagate sdk.dir to SAGE submodule so Android Studio / Gradle can find the SDK
// when building the included composite build.
val sageLocalProps = file("sage/local.properties")
if (!sageLocalProps.exists()) {
    val rootLocalProps = file("local.properties")
    if (rootLocalProps.exists()) {
        sageLocalProps.writeText(rootLocalProps.readText())
    }
}

includeBuild("sage/sage-build-logic")
includeBuild("sage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("$settingsDir/sage/gradle/libs.versions.toml"))
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "sage-sample"

include(
    ":apps:android",
    ":apps:jvm",
    ":core:bitmaps",
    ":core:settings",
    ":core:ui:components:api",
    ":core:viewmodel",
    ":features:browse",
    ":features:listitems",
    ":features:search",
    ":features:settings",
    ":features:welcome",
)
