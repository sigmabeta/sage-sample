# SAGE Sample

The canonical reference for building an app on [SAGE](../sage). It is deliberately small: one
Android app module (`:apps:android`), one Compose Desktop app module (`:apps:jvm`), one
Kotlin/JS browser app (`:apps:js`), and one Ktor server (`:apps:server`) that serves the browser
bundle and the sample's JSON API. Six multiplatform feature modules sit over shared `:core`
modules (a module-driven Home that shows a stable plan by default and can randomize 2–10 hero and
generated-image rows, Browse, a Content detail screen over seeded generated content, List Items —
as text-only, generated-image, and 50-item generated-image grid variants — Search, and a Settings
screen with persisted preferences). The data seams are split the way a real app would split them:
interfaces in `:core:repository:api`, deterministic in-process sources in `:core:repository:demo`
(Android + desktop), and an HTTP client in `:core:repository:remote` (browser, backed by
`apps:server`). A per-screen Paparazzi screenshot suite covers a phone/tablet device matrix, and
SAGE itself is a Git submodule consumed through a Gradle composite build.

## Requirements

| Tool | Version |
|------|---------|
| JDK | 17+ (CI uses Temurin 21) |
| Android SDK | platform 36 |
| Gradle | 9.5 (wrapper) |
| Git LFS | 3.x (the screenshot goldens are LFS objects) |
| Node | 22 (only for the `-Psage.js` browser build) |
| Yarn | 1.22.x (only for the `-Psage.js` browser build) |

`local.properties` must point at an Android SDK (`sdk.dir=...`). `settings.gradle.kts` copies it
into the `sage/` submodule so both builds resolve the SDK.

Screenshot goldens are stored in Git LFS. Run `git lfs install` once per machine (and
`git lfs pull` after cloning) so `verifyPaparazziDebug` sees PNGs rather than pointer files.

## Getting started

```bash
git clone --recurse-submodules <this repo>
cd sage-sample

./gradlew build             # compile, detekt, ktlint, lint, debug + release APK
./gradlew :apps:android:assembleDebug
./gradlew :apps:android:installDebug
./gradlew :apps:jvm:run     # run the Compose Desktop app
./gradlew :apps:server:run -Psage.js=true   # build the browser bundle and serve it on :8080
```

Both native apps accept the sample's deep links. The Android build registers `sagesample://`, and the
desktop app reads a launch argument, so either can open a pushed screen directly:

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "sagesample://browse/list-items" net.sigmabeta.sage.sample
./gradlew :apps:jvm:run --args="sagesample://browse/list-items"
```

The browser already deep-links from its URL (`/browse/list-items`); see
[Web app and server](#web-app-and-server).

The server jobs require Node 22 and Yarn 1.x on `PATH` (the Kotlin/JS plugin is configured to use
the system tools). Open <http://localhost:8080>; `SAGE_SAMPLE_PORT` and `SAGE_SAMPLE_BIND_HOST`
override the listen address. A production distribution is
`./gradlew :apps:server:installDist -Psage.js=true`.

A plain clone needs the submodule initialised first:

```bash
git submodule update --init --recursive
```

## Verification

CI is `.github/workflows/ci.yml`. Six jobs run on every push, pull request, and manual dispatch:

| Job | Command |
|-----|---------|
| `static-analysis` | `./gradlew ktlintCheck detekt --continue` |
| `unit-test` | `./gradlew test …jvmTest :apps:server:test --continue` (screenshot modules excluded; the JVM source sets of the KMP modules are listed explicitly) |
| `screenshot` | `./gradlew verifyPaparazziDebug --continue` |
| `android` | `./gradlew :apps:android:lintRelease :benchmark:compileBenchmarkKotlin :apps:android:assembleDebug :apps:android:assembleRelease` |
| `jvm-app` | `./gradlew :apps:jvm:assemble` |
| `web` | `./gradlew -Psage.js=true :apps:js:ktlintCheck :apps:js:detekt :apps:js:jsBrowserDistribution :apps:server:installDist` |

The compile-heavy jobs wait for `static-analysis`, so a style/rule break fails fast.
Reports, JUnit XML, the desktop JVM jar, Paparazzi diff images, and the APKs are uploaded as
workflow artifacts.

Locally, `scripts/ci/verify.sh` runs the same tasks sequentially, keeps going after a failure so
one run shows everything, and writes a summary with all reports to `build/verification/`:

```bash
scripts/ci/verify.sh                 # everything except the opt-in web suite
scripts/ci/verify.sh --web           # also build the browser bundle + server distribution
scripts/ci/verify.sh --skip-apps     # ktlint/detekt + unit tests only (fast inner loop)
scripts/ci/verify.sh static-analysis # just the named task(s)
scripts/ci/verify.sh --list          # task names
```

The `web` task needs Node 22 + Yarn 1.x and is much slower than the JVM tasks, so it is opt-in
locally (CI runs it in the separate `web` job).

Screenshots have their own loop:

```bash
./gradlew verifyPaparazziDebug                  # check the committed goldens
./gradlew recordPaparazziDebug                  # re-record after an intentional UI change
scripts/dev/paparazzi-diff.sh                   # rank changed goldens + write golden|new|diff montages
scripts/dev/paparazzi-diff.sh --top 10 --keep   # keep the re-render for review
```

The release APK is unsigned: the sample has no release signing config, so `assembleRelease`
produces an unsigned artifact that must be signed before installing.

### Performance benchmarks

A device macrobenchmark suite (`:benchmark`) measures UI scroll and app startup on the installed
`benchmark` build of `:apps:android`. UI scenarios use the app's own deep links, so each one starts
directly on the screen it measures. Results are written as `results-ui.json` / `results-startup.json`
in the run's additional test output directory; `:testing:perfbench-core` owns the schema and
`:testing:perfbench` the result diff.

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest       # both axes on every attached device
scripts/benchmark/device-ui.sh                           # gate UI scroll against a local baseline
scripts/benchmark/device-ui.sh --record                  # (re)record that device's baseline
scripts/benchmark/device-startup.sh --threshold 0.97     # gate startup with a custom threshold
scripts/ci/verify.sh --device-ui                         # the same run inside verify.sh
```

A device script exits `3` (a flagged `SKIP`, not a failure) when no device is attached. Use a
physical device for real numbers; see [`benchmark/README.md`](benchmark/README.md) for the scenario
table, startup first-content wiring, and device-driver notes.

Unit tests live beside each module (`src/test/kotlin`): plain JUnit in the feature modules,
`:core:viewmodel`, and the desktop app; `:core:bitmaps`, `:core:strings:real` and
`:core:ui:shell` use the KMP `jvmTest` source set. The per-feature screenshot modules
(`:features:<x>:screenshot`) use Paparazzi to compare hand-built `ListState` fixtures against
committed LFS goldens. No emulator or device is needed; there is no native target.

Review-time judgement calls are in `CODING_STANDARDS.md`. Agent worktrees and closure gates use
`scripts/dev/`:

```bash
scripts/dev/worktree-add.sh <name> [base-ref]   # branch + local.properties + reference-cloned SAGE
scripts/dev/worktree-rm.sh <dir>                # submodule-aware removal
scripts/dev/check-worktree-hygiene.sh [dir]     # clean tree, no stash, submodule pin on a remote
scripts/dev/check-ticket-gates.sh --feature <slug>  # resolved boxes/comments + verify + JUnit
```

## Updating SAGE

SAGE is pinned by the submodule commit, so updates are explicit:

```bash
git -C sage fetch origin main
git -C sage checkout origin/main
./gradlew build
git add sage
git commit -m "Bump SAGE"
```

If SAGE renames a module, update the `libs.sage.*` alias used by the module that depends on it
(the version catalog is SAGE's own `gradle/libs.versions.toml`).

## Layout

```
build-logic/                 the sample's convention plugins (included build)
  convention/                SageSampleScreenshotPlugin: the `sagesample.screenshot` id
apps/android/               Android application
  SampleApplication.kt      owns the Metro AppScope graph
  di/SampleAppGraph.kt      @DependencyGraph(AppScope::class) : ViewModelGraph
  di/SampleBindings.kt      app-side implementations of SAGE seams
  MainActivity.kt           installs LocalMetroViewModelFactory, hosts the Compose UI
  storage/PreferencesStorage.kt  DataStore-backed SAGE Storage (settings persistence)
  res/font/inter_*.otf      bundled Inter weights for the Settings font picker
  ui/AndroidPlatformBack.kt  Android system Back -> the shared Back policy
apps/jvm/                   Compose Desktop application (shares the feature modules)
  Main.kt                   builds the desktop Metro graph and opens the window
  JvmBuildInfo.kt           generated version/build time for the About rows
  di/JvmSampleAppGraph.kt   desktop @DependencyGraph(AppScope::class) : ViewModelGraph
  di/JvmSampleBindings.kt   JVM implementations of SAGE seams (shared strings, logging, dispatchers, storage)
  storage/JvmStorage.kt     properties-file-backed SAGE Storage (settings persistence)
  resources/fonts/inter_*.otf  bundled Inter weights for the Settings font picker
  ui/DesktopPlatformBack.kt  Escape/Backspace + mouse Back -> the shared Back policy
  ui/PersistedWindowState.kt  window size restored from Storage between runs
  ui/WindowFocusLifecycle.kt  window focus as the screen foreground lifecycle
  ui/navigation/            desktop Back-key detection (pure, unit-tested)
apps/js/                    Kotlin/JS browser application
  src/jsMain/kotlin/…/JsMain.kt  builds the web graph, mounts SampleApp in ComposeViewport
  di/WebSampleAppGraph.kt   browser @DependencyGraph(AppScope::class) : ViewModelGraph
  di/WebSampleBindings.kt   browser seams: strings, logging, localStorage, dispatchers, Ktor client
  storage/LocalStorageStorage.kt  window.localStorage-backed SAGE Storage
  navigation/BrowserHistoryNavigation.kt  pushState/popstate <-> SampleRoute
  ui/WebPlatformBack.kt     Escape -> the shared Back policy
  src/jsMain/resources/index.html  SPA shell (base href + splash)
apps/server/                Ktor server; serves the JS bundle and the JSON API
  Main.kt                   config -> embeddedServer(Netty)
  SampleServer.kt           route stack shared with the tests (API, JSON 404, SPA fallback)
  ServerConfig.kt           SAGE_SAMPLE_PORT / SAGE_SAMPLE_BIND_HOST
  build.gradle.kts          copies the dev bundle for `run`, the production bundle for installDist
core/repository/api/        KMP repository interfaces, wire DTOs + API route constants
core/repository/demo/       JVM deterministic sources bound by Android/desktop (and used by the server)
core/repository/remote/     Ktor-client repository implementations used by the browser app
core/bitmaps/               KMP Compose module shared by every app
  BitmapGenerator.kt        deterministic gradient ImageBitmap seeded by a source object
  GeneratedBitmap.kt        @Preview-facing composable that renders a generated bitmap
  GeneratedImageFetcher.kt  Coil fetcher for generated:// URLs, seeded by the request URL
core/models/api/            KMP domain models + Content detail route codec
core/nav/                   KMP destinations, route/Back/layout/chrome policy, URL + deep-link codec,
                            screen keys
core/strings/api/           SampleStringId: the one id vocabulary for all user-visible text
core/strings/real/          SampleStringProvider: the single id -> English table
core/ui/components/api/     KMP Compose renderer suite shared by every app
  ComposableMapping.kt      ListModel.Content: ListModel subtype -> composable dispatcher
  *.kt                      one renderer per SAGE ListModel (rows, headers, images, loading)
  SearchField.kt            the search box chrome (back/clear actions, constant 48 dp height)
  SearchContent.kt          app-owned search: adaptive grid with the field and status scrim
  subs/                     drawing/effect building blocks (CrossfadeImage, Flasher, TintedText)
  utils/                    focus, image-size and math helpers
  previews/                 shared sizing constants + Android @Preview harnesses
core/ui/theme/api/          SampleTheme + the shared Material 3 type scale
core/ui/shell/              KMP Compose app shell shared by every app
  SampleApp.kt              composition root: theme/font + platform seams + navigation host
  SampleNavHost.kt          TabNavigator shell: tab switch, pushed screens, Back application
  SampleScreens.kt          one Voyager Screen per destination
  SampleTabs.kt             per-tab retained Navigator, active-tab holder, shared Back local
  SampleNavEffects.kt       one-shot SageEvent -> app navigation seam (CompositionLocal)
  SampleNavScaffold.kt      top bar + bottom bar/rail driven by WidthClass
  SampleTopBar.kt           title/back from the active screen's SAGE title state
  SampleChrome.kt           Compose chrome controller + locals (policy is in :core:nav)
  ScreenLifecycle.kt        one foreground Resume/Pause pair per visible screen
  SamplePlatformBack.kt     platform Back seam (Android BackHandler, desktop keys/mouse)
  SampleScreenContainer.kt  per-screen ViewModelStore seam (desktop + web implementation in the shell)
  SampleBrowserNavigation.kt  browser history seam (null on Android/desktop)
  SampleDeepLink.kt         native one-shot deep-link seam + the shared host-side source
  PerScreenViewModelStore.kt  Voyager per-screen store used by desktop and web
  SampleNavHost.kt          also reports routes and applies browser-driven navigation
core/ui/previews/           Paparazzi/@Preview harness shared by the screenshot modules
  ScreenPreview.kt          ListScreenPreview / ScreenPreview: VM-free ListState rendering
  DevicePreviews.kt         light/dark x phone/foldable/tablet @Preview matrix
  PreviewTestUtils.kt       Paparazzi device matrix + DeviceConfig -> WidthClass
  fake/FakeModelGenerator.kt  deterministic domain models for fixtures
core/settings/              KMP preference vocabulary shared by every app
  ThemeMode.kt              light/dark/system, plus the system-dark resolution
  FontChoice.kt             standard platform family or bundled Inter
  SimulateDelay.kt          none / 1s / 2s / 5s duration for every background load
  SampleSettingsManager.kt  StateFlow preferences over SAGE's Storage seam
  SampleDelayManager.kt     duration-aware extension of SAGE's DelayManager
core/viewmodel/             KMP ViewModel base shared by every feature
  SampleListViewModel.kt    ListState -> ListStateActual, ActionSink, delayed background seam
features/home/             multiplatform feature module (no Android dependencies)
  HomeState.kt             ListState: one section slot per home module
  HomeViewModel.kt         Metro-contributed ViewModel: fans module flows into their slots
  HomeModule.kt            pluggable row contract + resolved section payload
  HomeModuleDefinition.kt  registered row type: typeId, priority, instance factory
  HomeModuleFactory.kt     builds the stable or randomized 2–10 instance plan
  HomeModuleBindings.kt    Metro set of HomeModuleDefinitions (`@Binds @IntoSet`)
  modules/                 BigImageModule (daily hero) and GridImagesModule (bounded row)
features/home/screenshot/  Paparazzi goldens for Home (content/loading/empty/error)
features/browse/            same shape; static destination rows (no loading/content)
features/browse/screenshot/ Paparazzi goldens for Browse
features/contentdetail/     detail layout: hero art, generated CTAs and label/value rows, related
                            scroller and image/name/caption items over one predetermined-seed item
features/contentdetail/screenshot/  Paparazzi goldens for Content detail
features/listitems/         List Items (NameCaption), List Items with Images
                            (ImageNameCaption over generated:// Coil art), and Grid of Images
                            (50 GridImage cells over generated:// Coil art)
features/listitems/screenshot/  Paparazzi goldens for all three List Items screens
features/search/            query + two deterministic generated result sets (list rows, grid)
features/search/screenshot/ Paparazzi goldens for Search
features/settings/          theme, font, simulate-delay and same-content rows, app version and build date
features/settings/screenshot/  Paparazzi goldens for Settings
benchmark/                  Android test APK: device UI-scroll and startup macrobenchmarks
  DeviceBenchmark.kt        fingerprint, deep-link launcher, per-axis result sink, progress logging
  UiScrollBenchmark.kt      one WARM scroll per core surface, started through its deep link
  StartupBenchmark.kt       cold/warm launch to Home's first content
testing/perfbench-core/     shared result contract: schema, scenarios, report math, diff guards
testing/perfbench/          diff CLI over two result files (the baseline gate reads its JSON)
scripts/benchmark/          per-axis device gates (device-ui.sh, device-startup.sh)
scripts/dev/                agent worktrees, ticket gates, worktree hygiene, Paparazzi diff review
CODING_STANDARDS.md         review-time rules (closed-set tests, intent mapping, text)
detekt-config.yml           ruleset read by the SAGE base plugins
sage/                       SAGE submodule (composite build)
```

## Module reference

[`docs/modules/`](docs/modules/README.md) has one page per Gradle module — its job, build plugins,
dependencies and why they are there, key types, runtime wiring, and tests. Use it when you need
module detail; this README is the cross-cutting architecture.

## How the wiring works

### Composite build

`settings.gradle.kts` includes SAGE, its build logic, and the sample's own build logic, and reuses
SAGE's version catalog:

```kotlin
includeBuild("sage/sage-build-logic")
includeBuild("build-logic")
includeBuild("sage")

versionCatalogs {
    create("libs") { from(files("$settingsDir/sage/gradle/libs.versions.toml")) }
}
```

There are no `dependencySubstitution` rules: Gradle matches SAGE's published coordinates
(`net.sigmabeta.sage:list`, `net.sigmabeta.sage.android:themes`, …) to the included projects
automatically. Add a module by using its catalog alias, e.g. `libs.sage.common.ui.listScreens`.

### Conventions

The root `build.gradle.kts` declares the plugins SAGE's convention plugins apply by id
(`android.library`, `kotlin.jvm`, `detekt`, `ktlint`) with `apply false`, so their runtime is on
the shared classpath. A feature module then only declares its convention:

```kotlin
plugins {
    alias(libs.plugins.sage.jvm)
    alias(libs.plugins.sage.di)
}
```

`detekt-config.yml` at the repo root is required: the SAGE base plugins configure detekt with the
consumer's `rootDir/detekt-config.yml`.

### Shared modules

`:core:strings:api`/`:real` own the `SampleStringId` vocabulary and the single English table;
`:core:ui:theme:api` owns `SampleTheme`; `:core:models:api` owns the domain types and the Content
detail route codec; `:core:nav` owns destinations plus the route/Back/layout/chrome policy and
screen keys; `:core:ui:shell` owns the composition root and the whole Compose/Voyager host. Both
apps and the screenshot harness consume them, so an app cannot drift from a golden or from the
other app.

### Screenshot tests

Each `:features:<x>:screenshot` module applies `sagesample.screenshot` from `build-logic/`, which
layers Paparazzi on the SAGE Android/Compose conventions. Its previews render hand-built
`ListState` fixtures through `:core:ui:previews` — never a ViewModel or repository — so goldens do
not depend on time, DI, or the demo delay. Fixtures get their domain rows from
`FakeModelGenerator`, and render with the shared `SampleStringProvider` and `SampleTheme`.

Goldens live beside the test in `src/test/snapshots/images/` and are Git LFS objects. CI checks them
out with LFS and runs `verifyPaparazziDebug`; the generic `test` task excludes the screenshot
modules so only that task owns the goldens. `scripts/dev/paparazzi-diff.sh` re-renders, ranks the
changed goldens by pixel difference, and writes `golden | new | diff` montages under
`build/paparazzi-review/`.

### Dependency injection — Metro, not Hilt

SAGE uses [Metro](https://github.com/ZacSweers/metro). There is no Hilt, KSP, or Dagger in this
app. The entry points:

- `@DependencyGraph(AppScope::class)` on `SampleAppGraph` declares the graph; it extends
  `ViewModelGraph`, which carries Metro's ViewModel multibinding map.
- `@SingleIn(AppScope::class)` marks app-lifetime bindings.
- `@BindingContainer` + `@ContributesTo(AppScope::class)` contributes a module from any module.
- `@Inject` (from `dev.zacsweers.metro`) marks injectable classes/constructors.
- `@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())` + `@ViewModelKey` puts a
  screen's view model in the ViewModel map, where `metroViewModel<...>()` finds it.
- `createGraphFactory<SampleAppGraph.Factory>().create(application)` builds it; Activity/Service
  code reads bindings from the graph rather than receiving member injection.

Every feature module applies SAGE's KMP conventions plus the Metro compiler plugin, so its view
model's constructor (and its ViewModel-map entry) is processed at compile time for every target. `:core:viewmodel` contributes the one `SampleMetroViewModelFactory`
binding that reads that map; each app installs it with `LocalMetroViewModelFactory`, and screens
call `metroViewModel<TheirViewModel>()`.

### Screens

SAGE splits a screen in two:

1. The feature owns the state machine. `HomeState : ListState` declares a title and a list of
   `ListModel`s; `HomeViewModel : SampleListViewModel<HomeState>` owns actions and analytics.
   `SampleListViewModel` (`:core:viewmodel`) is an `androidx.lifecycle.ViewModel` base with
   `uiStateActual` mapped from `ListState` — SAGE's plain `ListViewModelBrain` (and the graph
   accessor per screen) is a Dagger-era shell that Metro does not need.
2. The shared shell owns rendering. `:core:ui:shell`'s `SampleScreens` resolves the view model
   with `metroViewModel<HomeViewModel>()`, observes `uiStateActual`, reports its title through
   `LocalSampleTitleBarSink`, and passes the state to SAGE's `ListScreen` — or to `GridScreen`
   when `screenLayoutFor` sees more than one column. It supplies the shared `ListModel.Content`
   renderer (`:core:ui:components:api`) to draw each model. Unknown model types fall through to a
   spacer, so features can add their own models without breaking either app.
3. The shared screen owns the foreground pair: `Resume` when it enters composition,
   `Pause` when it leaves. `SampleListViewModel.sendAction` coalesces those transitions,
   so SAGE's extra title-change `Resume` does not run feature side effects twice.

Text goes through `StringProvider` and the shared `SampleStringId` enum (`:core:strings:api`), so
feature modules stay platform-agnostic. `:core:strings:real` is the single id → text table; every
app binds it and installs it at the composition root, and none owns a copy.

Home is module-driven. `HomeModule` is the pluggable row
contract: a stable id, a priority, an optional section header, and a
`Flow<LCE<HomeModuleSection>>`. Row types register as `HomeModuleDefinition`s; `HomeModuleBindings`
contributes them through Metro's `@Multibinds Set` + `@Binds @IntoSet`, so adding a row is one
binding line rather than a factory switch. `HomeModuleFactory` builds the plan for each screen:
the stable plan (the default) renders one instance of every registered definition in priority
order, while disabling "Home shows same content" produces the demo's randomized plan — between 2
and 10 instances, every definition represented, repeated types assigned unique ids and data-id
ranges. `HomeViewModel` collects every instance in parallel and patches only that module's slot in
`HomeState`; it also observes the preference and rebuilds the plan in place when it changes, so
the retained Home tab does not need an app restart.

`HomeState` gives each slot SAGE's standard loading and error rows and wraps multi-item rows in a
`HorizontalScrollerListModel`; a single-item row (the daily hero) keeps the full width. Each
instance gets a numbered section title and its own data-id range, so repeated module types
coexist without tripping SAGE's duplicate-id check. Each instance also passes its id to the
repository and its artwork URLs, so two instances of the same module render different titles and
generated art while re-visiting one instance reproduces its content. The Settings screen's
"Home shows same content" toggle switches between that stable plan and the randomized one; Home
observes the setting and swaps the plan without being recreated. The sample ships two modules:
`BigImageModule` picks one item per UTC day from the instance's generated catalog and renders it
as a width-capped `GridImageListModel`,
and `GridImagesModule` asks the repository for a bounded list and renders `GridImageListModel`
cells.

The List Items screens share one seeded generator (`DemoContent.sampleItems` in
`:core:repository:demo`, exposed through `SampleItemRepository`/`GridOfImagesRepository`). The
text-only variant maps each `SampleItem`
to `NameCaptionListModel`; the image variant maps the same data to `ImageNameCaptionListModel`
with a deterministic `generated://` URL in its `SourceInfo`; Grid of Images asks the generator
for 50 items and maps them to `GridImageListModel` cells at a 160 dp column width. The shared
`ImageNameCaptionListItem` and `GridImage` renderers load those URLs through Coil, so the artwork
comes from `:core:bitmaps`' fetcher rather than bundled bitmap assets.

Content Detail is a generic detail layout driven entirely by generated content. `ContentDetailState`
renders a hero image, a column of CTA rows whose text and icons both come from the seed, a block
of generated label/value rows, a horizontally scrolling "Related" row of `WideItemListModel`s, and
an "Items" list of `ImageNameCaptionListModel` rows. One `LCE<ContentDetail>` drives every section
through its own loading shape (`BIG_IMAGE`, no placeholder for the CTAs/details, `WIDE_ITEM` in a
scroller, `TEXT_CAPTION_IMAGE` for the items), and an error replaces the screen with a single
error row. The demo `ContentDetailRepository` (`:core:repository:demo`) builds the item from a
seed derived from the ID, so the content is random-looking but identical on every visit — the
"randomized content, predetermined seed" contract. When the ID came from a row, the detail's title
and hero artwork reuse that row's own name and artwork seed, so tapping a list row, grid cell, Home
tile or search result opens the item the user actually saw. Related rows and content parts embed
their parent ID (for example `content-detail:related:list-items:7:1`), so drilling into one also
keeps its name and thumbnail. The only fixed text on the screen is section chrome and the click
snackbar; titles, labels, CTA text, names and captions are generated.

Search keeps the same feature-state / app-rendering split, with a search shell: the
shared `SearchContent` overlays a `SearchField` on an adaptive results grid, with a status-bar scrim
and a back action in the bar. Because the field replaces the top bar, `SearchScreen` sets
`SampleScreenChrome(showTopBar = false)` on the app-owned `SampleChromeController`; the scaffold
animates the top bar away, and `SampleNavHost` resets the chrome to `Default` on every destination
change, so Search does not restore it on disposal. The field sends SAGE's `SearchQueryEntered` and
`SearchClearClicked`; `SearchViewModel` waits 300 ms after typing and ignores queries shorter than
three characters.
A settled query then runs two independent repository calls. The demo `SearchRepository` seeds
each call from the trimmed, lowercased query plus a list/grid salt, so the same query always produces
the same rows and `generated://` artwork, while each call independently picks a random count from
one to twenty results. The list call maps to `ImageNameCaptionListModel` rows and the grid call
to `GridImageListModel` cells; `SearchState` renders them as two headed sections, with per-section
loading and error rows. If both calls come back empty, the screen shows its no-results state.

### Web app and server

`:apps:js` is a Kotlin/JS browser executable gated behind `-Psage.js`. It builds a
`WebSampleAppGraph`, mounts the same `SampleApp` in a `ComposeViewport`, persists settings in
`window.localStorage`, and binds `:core:repository:remote` instead of the demo sources. Artwork is
drawn directly by `:core:bitmaps` (`LocalForceFakeImages`), so the browser needs no Coil fetcher.

`:apps:server` is a stateless Ktor/Netty server. It serves the webpack bundle from
`src/main/resources/static` with an SPA fallback (`/browse/list-items` and `/settings` both return
`index.html`; a missing asset is a real 404) and a JSON API under `/api` backed by the same
`DemoContent` generators the demo repositories use. Unknown `/api` paths return a JSON 404, never
the shell. `SAGE_SAMPLE_PORT` / `SAGE_SAMPLE_BIND_HOST` control the listen address.

`SampleRoute`/`SampleRouteUrl` (`:core:nav`) is the pure URL model:
`/home`, `/browse/list-items`, `/home/content-detail/list-items:7`; short links such as `/settings`
land on Home and are canonicalised. `BrowserHistoryNavigation` mirrors forward navigation with
`pushState`, applies `popstate` through `SampleShellNavigation.navigateTo`, and lets Back use
`history.back()` whenever an app entry exists behind the current URL. Android and desktop pass no
browser bridge, so their navigation behaviour is unchanged.

The same route model carries native deep links. `SampleDeepLink` decodes
`sagesample://browse/list-items` (the URI authority is the first route segment, so a link mirrors
the web path) and also accepts a bare path for desktop launch arguments. Android claims the scheme
in its manifest and offers the intent URI from `onCreate`/`onNewIntent`; desktop offers its first
parseable argument. Both use the shared `MutableSampleDeepLinkSource` from `:core:ui:shell`, and
`SampleNavHost` applies the pending route through the same `navigateTo` a browser URL uses.

The renderer suite keeps one module that re-exports SAGE's `ListModel` types and owns a renderer
for every SAGE model the sample uses, plus `subs/` building blocks and `@Preview` harnesses.
Accessibility labels are `SampleStringId`s
from the shared strings module; both composition roots install the graph's `StringProvider` into
`LocalSampleStringProvider`, so the renderers never touch `R.string`.

### Navigation

Navigation uses Voyager's `TabNavigator` (from SAGE's catalog). The shared `:core:ui:shell` owns
the tabbed shell; each app supplies only its platform seams:

- **Destinations** are in `SampleDestination`, which implements SAGE's `RouteDescriptor`.
- **Home, Browse and Search are tabs** (`SampleTabs.kt`), each owning a retained inner
  `Navigator` and a stable root screen. The `NavigationBar` on compact windows and the
  `NavigationRail` at 600 dp and wider highlight `tabNavigator.current`, so a pushed screen
  keeps its owning tab selected. Switching tabs leaves the outgoing tab's stack alone, so the
  tab-switch slide moves out exactly the screen the user was on; that tab is marked to reopen at
  its root the next time it is shown, and the reset is applied when it is composed again rather
  than on screen. Re-tapping the active tab returns it to its root in place. The tab you return to
  keeps its root view model and UI state (including the Search query and scroll position).
- **Back is one policy** (`backActionFor`): a pushed screen pops inside the active tab, Search
  at its root returns Home, and Home/Browse roots fall through to the host (Android finishes
  the activity; desktop has nothing above the shell). The top-bar arrow, platform Back, and
  `SageEvent.NavigateBack` all run it. Each app supplies a `SamplePlatformBack`: Android maps it
  to `BackHandler`, desktop to window Escape/Backspace plus the mouse back button.
- **Settings** is pushed from the hamburger onto the active tab.
- **List Items, List Items with Images, Grid of Images, and Content Detail** are pushed from
  Browse onto the active tab. All four are non-top-level routes, so the back arrow returns to the
  screen they came from rather than resetting the stack. Grid of Images declares
  `ColumnType.Regular(160)`, so the shared screen renders it with SAGE's `GridScreen` rather than
  `ListScreen`; Content Detail declares `ColumnType.Staggered(320)`, so it keeps the
  single-column list on a compact window and becomes SAGE's staggered grid when the window fits
  more than one column.
- **Feature navigation is translated, not re-implemented.** Features emit SAGE
  `SageEvent`s (`NavigateTo`, `NavigateSingleTopLevel`, `NavigateBack`) through
  `SampleNavEffects`; `SampleNavAction`/`navActionFor` map a known top-level route to
  `ShowTopLevel` (select that tab) and any other route to
  `Push` onto the active tab (Settings included), so a feature can reach Settings the same way
  the hamburger does. `NavigateBack` runs the shared Back policy, and unknown routes or event
  types are ignored.
- **Deep links share the browser's route model.** `:core:nav`'s `SampleDeepLink` decodes
  `sagesample://browse/list-items` (the URI authority is the first route segment, so a link mirrors
  the web path) and accepts a bare path for desktop launch arguments. Android's manifest claims
  the scheme, `MainActivity` offers the intent URI from `onCreate`/`onNewIntent`, and desktop offers
  its first parseable argument; each feeds a `MutableSampleDeepLinkSource` in `:core:ui:shell`.
  `SampleNavHost` applies the pending route through `SampleShellNavigation.navigateTo` — the same
  path a browser URL takes — and then consumes it, so rotation or a reopened window cannot replay
  the link.
- **The top bar comes from the active route.** The screen reports its SAGE
  `ListStateActual.title` through `LocalSampleTitleBarSink`; `topBarState` uses the reported
  `title` and `shouldShowBack` (falling back to the destination label and `isTopLevel` until
  the screen reports). `shouldShowBack = false` renders the hamburger (opens Settings);
  otherwise the back arrow pops. Screens may hide the bar with the app-owned
  `SampleChromeController` (Search does, from a `LaunchedEffect`); the scaffold animates it away,
  and `SampleNavHost` resets `SampleScreenChrome.Default` whenever the active route (tab plus the
  active tab's top screen) changes. Navigation-tab labels and icons stay app-owned in
  `SampleDestination`.
- **Width classes** are SAGE's: 600 dp (compact → medium) and 840 dp (medium → expanded).
  `SampleNavHost` computes the window's class once and provides it via `LocalSampleWidthClass`,
  so the nav suite (bottom bar vs rail) and list/grid layout agree. A screen's
  `ColumnType.One` always renders as `ListScreen`; so does any column type that fits at most
  one column (including a wide `Regular`/`Staggered` on a narrow window, where integer
  division yields 0). More columns use `GridScreen` with `staggered` / `allowHorizScroller`
  from the column type.
- **Each screen resolves its own view model** with `metroViewModel<...>()`. Voyager gives every
  screen its own `ViewModelStoreOwner`, so a view model is retained while its screen stays in the
  back stack (even while another screen covers it) and is cleared when the screen is popped or
  removed; popping and re-pushing a screen builds a fresh one. `TabNavigator` keeps each tab's
  inner navigator, so a tab root additionally survives switching away and back. Metro contributes
  only the factory (`@ContributesIntoMap` + `@ViewModelKey`); the instance lives in the Voyager
  store, which is why screen view models are not `@SingleIn`.

## Desktop app

`apps/jvm` renders the same UI with Compose Multiplatform desktop. It reuses the feature modules,
the shared `:core` modules and the shared `:core:ui:shell` host; `apps/jvm` keeps only the window,
the desktop graph/storage and the desktop platform seams. The seams differ:

- **Text:** every app binds the shared `SampleStringProvider` from `:core:strings:real`; there is no
  desktop text table and no id → `R.string` map.
- **Theme and font:** the shared `SampleApp` wraps `:core:ui:theme:api`'s
  `SampleTheme(darkTheme, fontFamily)`, with `darkTheme` selected by the persisted `ThemeMode`
  (light, dark, or follow the system). `FontChoice` selects `FontFamily.Default` or Inter; each app
  loads its own copies of the Inter OTFs (Android `res/font`, desktop classpath `fonts/`) and
  passes the family in. Both choices live in `SampleSettingsManager`, outside any screen's view
  model.
- **View model stores:** Voyager supplies per-screen `ViewModelStoreOwner`s only on Android.
  Desktop passes `DesktopSampleScreenContainer`, which hangs a store off a Voyager `ScreenModel`,
  so a screen keeps its view model while it stays in the back stack and clears it when popped.
  Each tab's inner navigator is retained by `TabNavigator`, so a tab root's store survives
  switching tabs here exactly as it does on Android.
- **Back and lifecycle:** `DesktopPlatformBack` feeds window Escape/Backspace and the mouse back
  button into the shared policy, and `rememberWindowFocusLifecycleOwner` turns window focus into
  the `Resume`/`Pause` pair screens expect.
- **Window size:** `rememberPersistedWindowState` restores the size the app was closed with
  through the same `Storage` seam, before the platform window exists, and saves it back while the
  window is floating. The position stays the platform's business.
- **Dispatchers and storage:** `JvmSampleBindings` provides `SageDispatchers` with
  `Dispatchers.Main` backed by `kotlinx-coroutines-swing` (the AWT event queue), plus the JVM
  `Hatchet`, analytics, `AppInfo`, and a properties-file `Storage` implementation.

The Metro graph is `JvmSampleAppGraph`; `Main.kt` builds it once and installs its
`metroViewModelFactory` above the window, exactly as `MainActivity` does on Android.

## Adding a feature

1. Create `features/<name>` with the `sage.jvm` + `sage.di` convention, and depend on
   `projects.core.viewmodel` plus the SAGE modules it needs.
2. Add `:features:<name>` to `settings.gradle.kts` and to the `api(...)` list in
   `core/ui/shell/build.gradle.kts` (that is what puts the feature's view model on both app
   graphs).
3. Write a `<Name>State : ListState` and a `<Name>ViewModel : SampleListViewModel<<Name>State>`,
   using a `<Name>StringId : SageStringId` enum for text. Annotate the view model with
   `@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())` + `@ViewModelKey`, and
   send `SageAction.InitNoArgs` from `init`.
4. Map the string ids in `SampleBindings`; no graph accessor is needed for the view model.
5. To make it reachable, add a `SampleDestination`, a `SampleScreen` (resolving it with
   `metroViewModel`), and a `screenFor` branch in `:core:ui:shell`'s `SampleTabs.kt` — plus a nav
   label in `:core:strings:real` and a `SampleTab` if it becomes a top-level tab.
