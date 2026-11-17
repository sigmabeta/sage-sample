plugins {
    // Android + JVM only: Room KMP has no JS target, so this module does not apply sage.kmp.js and
    // nothing in the browser app may depend on it.
    alias(libs.plugins.sage.kmp)
    // Room's compiler runs per target through KSP and generates each target's database constructor.
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "net.sigmabeta.sage.sample.database"
    }

    // Room's @ConstructedBy needs an `expect object` whose actuals KSP generates per target.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                api(libs.room.runtime)
                // The bundled SQLite driver serves both targets, so Android and the JVM run the same
                // SQLite build rather than the framework copy.
                api(libs.sqlite.bundled)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        named("jvmTest") {
            dependencies {
                implementation(libs.junit4)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

// The exported schema is the migration contract: every version's JSON is committed under schemas/.
ksp {
    arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.absolutePath)
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}
