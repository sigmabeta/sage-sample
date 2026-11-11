package net.sigmabeta.sage.sample.perfbench

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The physical device a result was measured on, plus the app build it measured.
 *
 * There is only one fingerprint class — the sample has no host performance axis, so every result
 * comes from a real Android device. [appVersion] makes a baseline visibly tied to the build it was
 * recorded from without needing a git process in the build.
 */
@Serializable
data class DeviceFingerprint(
    val manufacturer: String,
    val model: String,
    @SerialName("android_build") val androidBuild: String,
    val abi: String,
    @SerialName("thermal_status") val thermalStatus: String,
    @SerialName("app_version") val appVersion: String,
)

/** The raw device facts, decoupled from how they were probed so [deviceFingerprint] stays pure. */
data class DeviceEnvironment(
    val manufacturer: String,
    val model: String,
    val androidBuild: String,
    val abi: String,
    val thermalStatus: String,
    val appVersion: String,
)

/** Builds a [DeviceFingerprint] from probed facts. */
fun deviceFingerprint(environment: DeviceEnvironment): DeviceFingerprint = DeviceFingerprint(
    manufacturer = environment.manufacturer,
    model = environment.model,
    androidBuild = environment.androidBuild,
    abi = environment.abi,
    thermalStatus = environment.thermalStatus,
    appVersion = environment.appVersion,
)
