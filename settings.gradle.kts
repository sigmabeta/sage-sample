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
// The sample's own convention plugins (the screenshot module convention). It layers on the SAGE
// build logic above, so this must come after it.
includeBuild("build-logic")
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
    ":apps:server",
    ":core:bitmaps",
    ":core:models:api",
    ":core:nav",
    ":core:repository:api",
    ":core:repository:demo",
    ":core:repository:remote",
    ":core:settings",
    ":core:strings:api",
    ":core:strings:real",
    ":core:ui:components:api",
    ":core:ui:previews",
    ":core:ui:shell",
    ":core:ui:theme:api",
    ":core:viewmodel",
    ":features:browse",
    ":features:browse:screenshot",
    ":features:contentdetail",
    ":features:contentdetail:screenshot",
    ":features:home",
    ":features:home:screenshot",
    ":features:listitems",
    ":features:listitems:screenshot",
    ":features:search",
    ":features:search:screenshot",
    ":features:settings",
    ":features:settings:screenshot",
)

// The browser app (and the Kotlin/JS variants its shared deps expose via sage.kmp.js) is gated
// behind -Psage.js so ordinary Android/JVM builds and IDE syncs never configure Kotlin/JS. The
// server's web-bundle wiring is gated to match.
if (providers.gradleProperty("sage.js").orNull.toBoolean()) {
    include(":apps:js")
}
