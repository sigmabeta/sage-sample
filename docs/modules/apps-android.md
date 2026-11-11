# `:apps:android`

The Android application: entry point, Metro app graph and Android platform seams. The Compose/
Voyager shell itself is shared with the desktop app in `:core:ui:shell`.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.metro)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)   // applied directly: SAGE conventions target libraries
}
```

`namespace`/`applicationId` are `net.sigmabeta.sage.sample`; `compileSdk`/`targetSdk` 36, `minSdk`
26, JVM 17. Version comes from the root project (`0.1.0`). `buildConfig` is enabled because Settings'
About rows read `VERSION_NAME`, `VERSION_CODE` and `BUILD_TIME_MS` (captured at BuildConfig
generation). Both build types are unminified; release is unsigned.

## Entry points

| File | Role |
|---|---|
| `SampleApplication.kt` | Creates the app-scoped `SampleAppGraph` lazily; registers `generatedImageLoader` as Coil's singleton; enables SAGE perf measurement in debug; holds the app-scoped `deepLinks` source. |
| `MainActivity.kt` | Installs `LocalMetroViewModelFactory` and `LocalSampleStringProvider` above `SampleApp`, supplies `finish()` as the outer Back action, and offers `sagesample://` intent links from `onCreate`/`onNewIntent` to the shell. |
| `di/SampleAppGraph.kt` | `@DependencyGraph(AppScope::class) : ViewModelGraph`. Exposes `settingsManager` and `stringProvider`; provides the application `Context`. |
| `di/SampleBindings.kt` | `@BindingContainer @ContributesTo(AppScope)` — binds the shared `SampleStringProvider`, plus `Hatchet`, `Analytics`, `Storage` (DataStore), `AppInfo`. The app owns no text table. |
| `storage/PreferencesStorage.kt` | Android `Storage` over Preferences DataStore (`sage_sample_settings`); disk-dispatched writes, flow reads. |

The shared `MetroViewModelFactory` binding lives in `:core:viewmodel`, so both graphs resolve it
without a per-app copy.

## UI shell

`MainActivity` installs the graph's `MetroViewModelFactory` and `StringProvider`, then calls the
shared `SampleApp` from `:core:ui:shell`, passing the Android platform seams:

| Seam | Android value |
|---|---|
| `platformBack` | `AndroidPlatformBack` (`ui/AndroidPlatformBack.kt`), an `androidx.activity.compose.BackHandler`. |
| `interFontFamily` | the three Inter OTF weights from `res/font`. |
| `screenContainer` | the default passthrough (Voyager already supplies per-screen stores). |
| `screenLifecycleOwner` | `null` → the activity's `LocalLifecycleOwner`. |
| `deepLinkSource` | `SampleApplication.deepLinks`, offered from `MainActivity`'s VIEW intents. |
| `onOuterBack` | `finish()`. |

Deep links: the manifest claims the `sagesample` scheme (`singleTop`, so a link tapped while the
shell is on top arrives in `onNewIntent`). `MainActivity` decodes `intent.dataString` with
`:core:nav`'s `SampleDeepLink` and offers it to the app-scoped `MutableSampleDeepLinkSource`; the
shell applies it through the same external-navigation path as a browser URL and consumes it. A
recreated activity only re-offers the link when `savedInstanceState` is null, so rotation does not
reset the stack.

```sh
adb shell am start -a android.intent.action.VIEW \
  -d "sagesample://browse/list-items" net.sigmabeta.sage.sample
```

The destinations, route mapping, Back policy, layout selection, chrome derivation and screen keys
live in `:core:nav`; the screens, tabs and nav host live in `:core:ui:shell`. The app keeps no
navigation code of its own.

Navigation facts worth knowing:

- Popping the tab root pops the shell (Android finishes the activity).
- Search hides the top bar; `SampleNavHost` resets chrome on every route change, so Search does not
  restore it on disposal.
- `ContentDetailScreen` keys its Voyager screen with `:core:nav`'s `contentDetailScreenKey`, so each
  ID gets its own view model and load.
- Layout is derived by `screenLayoutFor`: `ColumnType.One` or at most one computed column renders
  `ListScreen`; more columns renders `GridScreen` (staggered when the column type asks).

## Dependencies

`:core:ui:shell` (which re-exports the six feature modules, `:core:nav`, `:core:models:api`,
`:core:strings:api`, `:core:viewmodel`, `:core:settings`, `:core:ui:components:api`,
`:core:ui:theme:api` and `:core:bitmaps`), plus the in-process data source
`:core:repository:demo` (its Metro bindings supply every `*Repository`) and the Android seams this
module implements directly: `:core:strings:real`, `:core:settings`, `:core:ui:components:api`,
`:core:bitmaps`,
`metrox-viewmodel(-compose)`, SAGE di/appinfo/analytics/logging/coroutines/ui-strings/perf and the
Android logging/coroutines/strings modules, AndroidX DataStore Preferences, activity-compose and
Coil core.

## Tests

The Android module has no unit tests of its own: the shared chrome and tab tests moved to
`:core:ui:shell`'s `jvmTest`, and the pure shell policy tests (`SampleDestinationTest`,
`SampleNavActionTest`, `SampleBackActionTest`, `SampleScreenLayoutTest`, `SampleTopBarStateTest`,
`SampleChromePolicyTest`, `SampleContentDetailScreenKeyTest`, `ScreenLifecycleTest`) live once in
`:core:nav`.

## Verify

```sh
./gradlew :apps:android:assembleDebug
./gradlew :apps:android:lintRelease :apps:android:assembleRelease
```

Do not boot an AVD; hand device testing to the user. The release APK needs signing before install.
