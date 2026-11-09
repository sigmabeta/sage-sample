// The sample's convention plugins layer on the SAGE base plugins (sage.android,
// sage.compose.android), which they apply by id — and a plugin applied programmatically resolves
// against the DEFINING build's classpath, so this build must include sage-build-logic and depend on
// its convention project.
includeBuild("../sage/sage-build-logic")

dependencyResolutionManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            // Reuse SAGE's catalog so the screenshot convention resolves the same plugin/library
            // coordinates as the app modules (Paparazzi in particular is pinned there).
            from(files("../sage/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "sage-sample-build-logic"
include(":convention")
