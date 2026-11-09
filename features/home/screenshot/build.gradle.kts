plugins {
    id("sagesample.screenshot")
}

dependencies {
    // HomeState, HomeSectionState, HomeModuleSection and SampleStringId.
    implementation(projects.features.home)
}
