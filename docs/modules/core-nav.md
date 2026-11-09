# `:core:nav`

The navigation shell's pure policy, shared by both apps.

```kotlin
plugins {
    alias(libs.plugins.sage.jvm)
}
```

The Android and desktop apps used to carry identical copies of the destinations, route mapping, Back
policy, layout selection, top-bar derivation, tab keys and screen keys — plus duplicate tests. Those
decisions live here now; the Compose/Voyager host stays app-side.

## What lives here

| File | Contents |
|---|---|
| `SampleDestination.kt` | Every destination: `destName`, `SampleStringId` label, icon, top-level flag, `fromRoute`, `topLevel`. |
| `SampleNavAction.kt` | `SampleNavAction` (`ShowTopLevel` / `Push` / `Pop`) and `navActionFor(SageEvent)`. |
| `SampleBackAction.kt` | `SampleBackAction` (`Pop` / `ShowTab` / `Outer`) and `backActionFor(tab, canPop)`. |
| `SampleScreenLayout.kt` | `SampleScreenLayout` and `screenLayoutFor(columnType, widthClass)`. |
| `SampleChrome.kt` | `SampleScreenChrome`, `SampleTopBarLeading`, `SampleTopBarState`, `topBarState`, `routeKeyFor`, generic `activeScreenFor`, `widthClassFromDp`. |
| `SampleScreenKeys.kt` | `contentDetailScreenKey(contentId)`. |
| `SampleLifecycle.kt` | `foregroundActionFor(Lifecycle.Event)`. |

## What stays out of `:core:nav`

Compose and Voyager specifics that are genuinely host code now live in `:core:ui:shell`, shared by
both apps:

- `SampleChromeController`, the composition locals and `SampleTitleBarSink` (Compose state).
- `SampleTab`/`sampleTabs` (Voyager `Tab`, per-screen ViewModel stores).
- `SampleNavHost`, `SampleNavScaffold`, `SampleTopBar`, `SampleScreens`, `SampleNavEffects`.
- Platform Back (Android `BackHandler`, desktop key handling) enters through
  `SamplePlatformBack`; the app supplies the implementation.

Navigation labels are **not** host code: `SampleDestination.label` is a `SampleStringId`, and each
app resolves it through the shared `StringProvider`.

## Tests

The nine duplicated test classes moved here once: `SampleDestinationTest`, `SampleNavActionTest`,
`SampleBackActionTest`, `SampleScreenLayoutTest`, `SampleTopBarStateTest`,
`SampleChromePolicyTest`, `SampleContentDetailScreenKeyTest`, `ScreenLifecycleTest`. The shared
`SampleChromeTest`/`SampleTabsTest` live in `:core:ui:shell`; the apps keep only their platform
tests.

## Module facts

- **Plugin:** `sage.jvm` (pure JVM)
- **Source set:** `src/main/kotlin`, tests in `src/test/kotlin`
- **Dependencies:** `:core:models:api`, `:core:strings:api`, SAGE `nav`, `appcomm`, `list`,
  `ui.components`, `ui.iconsApi`, and `androidx.lifecycle:lifecycle-common` (all `api`).
- **Consumers:** `:apps:android`, `:apps:jvm`.
