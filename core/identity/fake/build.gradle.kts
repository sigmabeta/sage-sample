plugins {
    // Plain JVM on purpose: the fakes' callers are JVM test source sets (`apps:server`,
    // `:core:repository:demo`), and building this module proves `:core:identity`'s API is usable
    // from a non-multiplatform module. Depend on it with `testImplementation` only.
    alias(libs.plugins.sage.jvm)
}

dependencies {
    api(projects.core.identity)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
