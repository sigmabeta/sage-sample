plugins {
    // Pure JVM: the Android and desktop apps bind these in-process sources, and `apps:server`
    // uses the same generators to answer the web client's JSON requests. The browser client never
    // depends on this module — it talks to the server instead.
    alias(libs.plugins.sage.jvm)
    // Metro compiler plugin, so each demo source is contributed into the app graph.
    alias(libs.plugins.metro)
}

dependencies {
    api(projects.core.repository.api)
    // The demo sources read the Settings demo outcome (content/empty/error) per call.
    api(projects.core.settings)
    implementation(libs.sage.common.di)

    // `StringGenerator` — SAGE's seeded fake-data generator. It lives in SAGE's JVM source set,
    // which is why the demo data source is JVM-only while the contracts are multiplatform.
    api(libs.sage.common.ui.strings)
    api(libs.sage.common.appcomm)

    // The in-process identity source: Users and Sessions through `:core:identity`'s rules, over a
    // `SampleDatabase` the app provides (this module never builds one), the Session token kept in
    // SAGE Storage, and the slow PBKDF2 work moved onto SAGE's computation dispatcher.
    api(projects.core.identity)
    implementation(libs.sage.common.storage.common)
    implementation(libs.sage.common.coroutines)

    testImplementation(projects.core.identity.fake)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
