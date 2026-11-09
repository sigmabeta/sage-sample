plugins {
    alias(libs.plugins.sage.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("net.sigmabeta.sage.sample.server.MainKt")
    applicationName = "sage-sample-server"
}

// The web bundle is copied into the server's static resources. When -Psage.js is set, the JS app
// is part of the build and `run`/`installDist` produce a server that serves it. Without the flag
// the server is API-only (useful for route tests and IDE syncs that skip Kotlin/JS).
val jsModuleAvailable = providers.gradleProperty("sage.js")
    .map { it.toBoolean() }.getOrElse(false)

// Only `run` wants the development bundle. Shippable distributions copy the production bundle
// through installProductionJsBundle; pulling both in one invocation makes the two JS compile-sync
// tasks share `build/js/packages/.../kotlin`, which Gradle rejects as an implicit dependency.
val runRequested = gradle.startParameter.taskNames.any {
    it == ":apps:server:run" || it.endsWith(":apps:server:run")
}
val copyJsBundle = tasks.register<Copy>("copyJsBundle") {
    description = "Copies the apps/js development bundle into the server's static resources (run only)."
    group = "build"
    if (jsModuleAvailable && runRequested) {
        dependsOn(":apps:js:jsBrowserDevelopmentExecutableDistribution")
        from(project(":apps:js").layout.buildDirectory.dir("dist/js/developmentExecutable"))
        into(layout.buildDirectory.dir("generated/static/static"))
    }
}

// Shippable distributions always carry the production bundle.
val installProductionJsBundle = tasks.register<Copy>("installProductionJsBundle") {
    description = "Copies the production apps/js bundle into the server's static resources."
    group = "build"
    if (jsModuleAvailable) {
        dependsOn(":apps:js:jsBrowserDistribution")
        from(project(":apps:js").layout.buildDirectory.dir("dist/js/productionExecutable"))
        into(layout.buildDirectory.dir("generated/static/static"))
    } else {
        doFirst {
            error(
                "Cannot assemble a shippable server distribution without the web bundle. " +
                    "Re-run with -Psage.js=true (apps:js is gated behind that flag).",
            )
        }
    }
}

sourceSets {
    main {
        resources.srcDir(layout.buildDirectory.dir("generated/static"))
    }
}

tasks.named("processResources") {
    dependsOn(copyJsBundle)
    mustRunAfter(installProductionJsBundle)
}

listOf("installDist", "distZip", "distTar").forEach { taskName ->
    tasks.named(taskName) {
        dependsOn(installProductionJsBundle)
    }
}

dependencies {
    implementation(projects.core.repository.api)
    // The same deterministic generators the Android/desktop demo sources use. The server never
    // constructs the Settings-aware demo repositories; it calls DemoContent directly.
    implementation(projects.core.repository.demo)
    implementation(projects.core.models.api)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit4)
    testImplementation("io.ktor:ktor-server-test-host:${libs.versions.ktor.get()}")
}
