# `:core:nav`

The navigation shell's pure policy and URL codec, shared by every app.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
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
| `SampleRoute.kt` | `SampleRoute` (tab + screen + optional content id) and `SampleRouteUrl` encode/decode. |
| `SampleDeepLink.kt` | `SampleDeepLink.routeFor(value)`: `sagesample://` links (authority = first route segment) and bare paths, decoded by `SampleRouteUrl`. |
| `SampleLifecycle.kt` | `foregroundActionFor(Lifecycle.Event)`. |

## What stays out of `:core:nav`

Compose and Voyager specifics that are genuinely host code now live in `:core:ui:shell`, shared by
every app:

- `SampleChromeController`, the composition locals and `SampleTitleBarSink` (Compose state).
- `SampleTab`/`sampleTabs` (Voyager `Tab`, per-screen ViewModel stores).
- `SampleNavHost`, `SampleNavScaffold`, `SampleTopBar`, `SampleScreens`, `SampleNavEffects`.
- Platform Back (Android `BackHandler`, desktop key handling) enters through
  `SamplePlatformBack`; the app supplies the implementation.

Navigation labels are **not** host code: `SampleDestination.label` is a `SampleStringId`, and each
app resolves it through the shared `StringProvider`.

## Tests

The nine duplicated policy test classes moved here once: `SampleDestinationTest`,
`SampleNavActionTest`, `SampleBackActionTest`, `SampleScreenLayoutTest`, `SampleTopBarStateTest`,
`SampleChromePolicyTest`, `SampleContentDetailScreenKeyTest`, `ScreenLifecycleTest` — plus
`SampleRouteUrlTest`, which pins `/home`, `/browse/list-items`, `/home/content-detail/{id}`,
tab-less short links, canonical round trips, and malformed paths, and `SampleDeepLinkTest`, which
pins the native `sagesample://` form, bare launch-argument paths, rejected schemes/routes and a
canonical round trip. The shared `SampleChromeTest`/`SampleTabsTest` live in `:core:ui:shell`; the
apps keep only their platform tests.

## Module facts

- **Plugin:** `sage.kmp` + `sage.kmp.js` (pure Kotlin, no Compose)
- **Source sets:** `src/commonMain/kotlin`, tests in `src/jvmTest/kotlin`
- **Dependencies:** `:core:models:api`, `:core:strings:api`, SAGE `nav`, `appcomm`, `list`,
  `ui.components`, `ui.iconsApi`, and `androidx.lifecycle:lifecycle-common` (all `api`).
- **Consumers:** `:apps:android`, `:apps:jvm`, `:apps:js` (the server does not navigate).
