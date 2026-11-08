plugins {
    alias(libs.plugins.sage.jvm)
}

dependencies {
    // `ContentAction` carries an Icon; every feature that builds a detail row needs the same type.
    api(libs.sage.common.ui.iconsApi)

    testImplementation(libs.junit4)
}
