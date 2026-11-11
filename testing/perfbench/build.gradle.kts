plugins {
    alias(libs.plugins.sage.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// A measurement tool, not a shipped app: an explicit `run` task rather than the `application`
// plugin (no distribution or start scripts). The device axes live in the Android `:benchmark`
// module; this CLI only reads and compares their results.
tasks.register<JavaExec>("run") {
    group = "application"
    description = "Run the perfbench CLI (diff)."
    mainClass.set("net.sigmabeta.sage.sample.perfbench.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`
}

dependencies {
    implementation(projects.testing.perfbenchCore)

    testImplementation(libs.junit4)
}
