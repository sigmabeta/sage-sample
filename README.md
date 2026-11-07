# SAGE Sample

The canonical reference for building an app on [SAGE](../sage). It is deliberately small: one
Android app module (`:apps:android`), one Compose Desktop app module (`:apps:jvm`), six pure-JVM feature
modules over shared `:core` modules (a module-driven Home that randomizes 2–10 hero and
generated-image rows per visit, Browse, a Content detail screen over seeded generated content,
List Items — as text-only, generated-image, and 50-item generated-image grid variants —
Search, and a Settings screen with persisted preferences), and SAGE itself as a Git submodule
consumed through a Gradle composite build.

## Requirements

| Tool | Version |
|------|---------|
| JDK | 17+ (CI uses Temurin 21) |
| Android SDK | platform 36 |
| Gradle | 9.5 (wrapper) |

`local.properties` must point at an Android SDK (`sdk.dir=...`). `settings.gradle.kts` copies it
into the `sage/` submodule so both builds resolve the SDK.

## Getting started

```bash
git clone --recurse-submodules <this repo>
cd sage-sample

./gradlew build             # compile, detekt, ktlint, lint, debug + release APK
./gradlew :apps:android:assembleDebug
./gradlew :apps:android:installDebug
./gradlew :apps:jvm:run     # run the Compose Desktop app
```

A plain clone needs the submodule initialised first:

```bash
git submodule update --init --recursive
```

## Verification

CI is `.github/workflows/ci.yml`. Four jobs run on every push, pull request, and manual dispatch:

| Job | Command |
|-----|---------|
| `static-analysis` | `./gradlew ktlintCheck detekt --continue` |
| `unit-test` | `./gradlew test :core:bitmaps:jvmTest --continue` |
| `android` | `./gradlew :apps:android:lintRelease :apps:android:assembleDebug :apps:android:assembleRelease` |
| `jvm-app` | `./gradlew :apps:jvm:assemble` |

The compile-heavy jobs wait for `static-analysis`, so a style/rule break fails fast.
Reports, JUnit XML, the desktop JVM jar, and the APKs are uploaded as workflow artifacts.

Locally, `scripts/ci/verify.sh` runs the same tasks sequentially, keeps going after a failure so
one run shows everything, and writes a summary with all reports to `build/verification/`:

```bash
scripts/ci/verify.sh                 # everything
scripts/ci/verify.sh --skip-apps     # ktlint/detekt + unit tests only (fast inner loop)
scripts/ci/verify.sh static-analysis # just the named task(s)
scripts/ci/verify.sh --list          # task names
```

The release APK is unsigned: the sample has no release signing config, so `assembleRelease`
produces an unsigned artifact that must be signed before installing.

Unit tests live beside each module (`src/test/kotlin`): plain JUnit in the feature modules,
`:core:viewmodel`, and the desktop app; `:core:bitmaps` uses the KMP `jvmTest` source set;
Android unit tests in `:apps:android` for app-owned logic such as route mapping, chrome derivation, and
layout selection. No emulator is needed; there is no Paparazzi, device, or native target.

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
apps/android/               Android application
  SampleApplication.kt      owns the Metro AppScope graph
  di/SampleAppGraph.kt      @DependencyGraph(AppScope::class) : ViewModelGraph
  di/SampleMetroViewModelFactory.kt  the factory behind metroViewModel()
  di/SampleBindings.kt      app-side implementations of SAGE seams
  MainActivity.kt           installs LocalMetroViewModelFactory, hosts the Compose UI
  storage/PreferencesStorage.kt  DataStore-backed SAGE Storage (settings persistence)
  res/font/inter_*.otf      bundled Inter weights for the Settings font picker
  ui/SampleApp.kt           settings-driven theme + font + navigation host
  ui/GeneratedBitmapPreview.kt  @Preview for the shared generated placeholder art
  ui/navigation/
    SampleDestination.kt    routes + tab metadata (SAGE RouteDescriptor)
    SampleScreens.kt        one Voyager Screen per destination
    SampleTabs.kt           per-tab retained Navigator, active-tab holder, shared Back local
    SampleNavHost.kt        TabNavigator shell: tab switch, pushed screens, Back application
    SampleNavEffects.kt     one-shot SageEvent -> app navigation seam (CompositionLocal)
    SampleNavAction.kt      pure SageEvent -> ShowTopLevel / Push / Pop policy
    SampleBackAction.kt     pure tab + canPop -> Pop / ShowTab / Outer policy
    SampleSystemBack.kt     platform Back seam (Android BackHandler; desktop no-op)
    SampleNavScaffold.kt    top bar + bottom bar/rail driven by WidthClass
    SampleTopBar.kt         title/back from the active screen's SAGE title state
    SampleChrome.kt         pure top-bar/width-class/layout derivation + locals
apps/jvm/                   Compose Desktop application (shares the feature modules)
  Main.kt                   builds the desktop Metro graph and opens the window
  JvmBuildInfo.kt           generated version/build time for the About rows
  SampleStrings.kt          desktop text, mirroring apps/android/src/main/res/values/strings.xml
  SampleStringProvider.kt   feature SageStringId -> desktop text
  di/JvmSampleAppGraph.kt   desktop @DependencyGraph(AppScope::class) : ViewModelGraph
  di/JvmSampleBindings.kt   JVM implementations of SAGE seams (strings, logging, dispatchers, storage)
  di/JvmMetroViewModelFactory.kt  the factory behind metroViewModel() on desktop
  storage/JvmStorage.kt     properties-file-backed SAGE Storage (settings persistence)
  resources/fonts/inter_*.otf  bundled Inter weights for the Settings font picker
  ui/SampleApp.kt           settings-driven theme + font + navigation host
  ui/GeneratedBitmapPreview.kt  @Preview for the shared generated placeholder art
  ui/PerScreenViewModelStore.kt  per-screen ViewModelStore (Voyager only does this on Android)
  ui/navigation/            same destinations, chrome and back stack as :apps:android
core/bitmaps/               KMP Compose module shared by both apps
  BitmapGenerator.kt        deterministic gradient ImageBitmap seeded by a source object
  GeneratedBitmap.kt        @Preview-facing composable that renders a generated bitmap
  GeneratedImageFetcher.kt  Coil fetcher for generated:// URLs, seeded by the request URL
core/ui/components/api/     KMP Compose renderer suite shared by both apps
  ComposableMapping.kt      ListModel.Content: ListModel subtype -> composable dispatcher
  SampleTypography.kt       shared Material 3 type scale, parameterised by the chosen font
  *.kt                      one renderer per SAGE ListModel (rows, headers, images, loading)
  SearchField.kt            the search box chrome (back/clear actions, constant 48 dp height)
  SearchContent.kt          Chipbox-style search: adaptive grid with the field and status scrim
  subs/                     drawing/effect building blocks (CrossfadeImage, Flasher, TintedText)
  utils/                    focus, image-size and math helpers
  previews/                 shared sizing constants + Android @Preview harnesses
core/settings/              pure-JVM preference vocabulary shared by both apps
  ThemeMode.kt              light/dark/system, plus the system-dark resolution
  FontChoice.kt             standard platform family or bundled Inter
  SimulateDelay.kt          none / 1s / 2s / 5s duration for every background load
  SampleSettingsManager.kt  StateFlow preferences over SAGE's Storage seam
  SampleDelayManager.kt     duration-aware extension of SAGE's DelayManager
core/viewmodel/             pure-JVM ViewModel base shared by every feature
  SampleListViewModel.kt    ListState -> ListStateActual, ActionSink, delayed background seam
features/home/             pure-JVM feature module (no Android dependencies)
  HomeState.kt             ListState: one section slot per home module
  HomeViewModel.kt         Metro-contributed ViewModel: fans module flows into their slots
  HomeModule.kt            pluggable row contract + resolved section payload
  HomeModuleFactory.kt     per-screen randomized 2–10 instance plan
  HomeModuleBindings.kt    provides the randomized List<HomeModule>
  HomeStringId.kt          SageStringId keys; the app maps them to R.string
  modules/                 BigImageModule (daily hero) and GridImagesModule (bounded row)
  DemoHomeRepository.kt    seeded catalog + generated:// artwork
features/browse/            same shape; static destination rows (no loading/content)
features/contentdetail/     detail layout: hero art, generated CTAs and label/value rows, related
                            scroller and image/name/caption items over one predetermined-seed item
features/listitems/         List Items (NameCaption), List Items with Images
                            (ImageNameCaption over generated:// Coil art), and Grid of Images
                            (50 GridImage cells over generated:// Coil art)
features/search/            query + two deterministic generated result sets (list rows, grid)
features/settings/          theme, font, simulate-delay and same-content rows, app version and build date
scripts/dev/                agent worktrees, ticket gates, worktree hygiene
CODING_STANDARDS.md         review-time rules (closed-set tests, intent mapping, text)
detekt-config.yml           ruleset read by the SAGE base plugins
sage/                       SAGE submodule (composite build)
```

## How the wiring works

### Composite build

`settings.gradle.kts` includes SAGE and its build logic, and reuses SAGE's version catalog:

```kotlin
includeBuild("sage/sage-build-logic")
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

The feature module applies `sage.di`, so its view model's constructor (and its ViewModel-map entry)
is processed at compile time. `SampleMetroViewModelFactory` is the contributed
`MetroViewModelFactory` that reads that map; `MainActivity` installs it with
`LocalMetroViewModelFactory`, and screens call `metroViewModel<TheirViewModel>()`.

### Screens

SAGE splits a screen in two:

1. The feature owns the state machine. `HomeState : ListState` declares a title and a list of
   `ListModel`s; `HomeViewModel : SampleListViewModel<HomeState>` owns actions and analytics.
   `SampleListViewModel` (`:core:viewmodel`) is an `androidx.lifecycle.ViewModel` base with
   `uiStateActual` mapped from `ListState` — SAGE's plain `ListViewModelBrain` (and the graph
   accessor per screen) is a Dagger-era shell that Metro does not need.
2. The app owns rendering. `SampleScreens` resolves the view model with
   `metroViewModel<HomeViewModel>()`, observes `uiStateActual`, reports its title
   through `LocalSampleTitleBarSink`, and passes the state to SAGE's `ListScreen` — or to
   `GridScreen` when `screenLayoutFor` sees more than one column. It supplies the shared
   `ListModel.Content` renderer (`:core:ui:components:api`) to draw each model. Unknown model
   types fall through to a spacer, so features can add their own models without breaking the app.
3. The shared screen owns the foreground pair: `Resume` when it enters composition,
   `Pause` when it leaves. `SampleListViewModel.sendAction` coalesces those transitions,
   so SAGE's extra title-change `Resume` does not run feature side effects twice.

Text goes through `StringProvider` and a feature-owned `SageStringId` enum, so feature modules stay
platform-agnostic; `SampleBindings` is the only place that knows about `R.string`.

Home is module-driven, the way Chipbox's home screen is. `HomeModule` is the pluggable row
contract: a stable id, a priority, an optional section header, and a
`Flow<LCE<HomeModuleSection>>`. `HomeModuleFactory` builds a fresh randomized plan for each
screen — between 2 and 10 instances of the two module types, both always represented, in random
order. `HomeModuleBindings` provides that `List<HomeModule>` to the graph, and `HomeViewModel`
collects every instance in parallel and patches only that module's slot in `HomeState`.
`HomeState` gives each slot SAGE's standard loading and error rows and wraps multi-item rows in a
`HorizontalScrollerListModel`; a single-item row (the daily hero) keeps the full width. Each
instance gets a numbered section title and its own data-id range, so repeated module types
coexist without tripping SAGE's duplicate-id check. Each instance also passes its id to the
repository and its artwork URLs, so two instances of the same module render different titles and
generated art while re-visiting one instance reproduces its content. The Settings screen's
"Home shows same content" toggle swaps the plan's random source for a fixed seed, so every visit
reproduces the same rows. The sample ships two modules: `BigImageModule` picks one item per UTC
day from the instance's generated catalog and renders it as a width-capped `GridImageListModel`,
and `GridImagesModule` asks the repository for a bounded list and renders `GridImageListModel`
cells.

The List Items screens share one seeded generator. The text-only variant maps each `SampleItem`
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
error row. `DemoContentDetailRepository` builds the item from a fixed seed
(`CONTENT_DETAIL_SEED`), so the content is random-looking but identical on every visit — the
"randomized content, predetermined seed" contract. The only fixed text on the screen is section
chrome and the click snackbar; titles, labels, CTA text, names and captions are generated.

Search keeps the same feature-state / app-rendering split, with a Chipbox-style search shell: the
shared `SearchContent` overlays a `SearchField` on an adaptive results grid, with a status-bar scrim
and a back action in the bar. Because the field replaces the top bar, `SearchScreen` sets
`SampleScreenChrome(showTopBar = false)` on the app-owned `SampleChromeController`; the scaffold
animates the top bar away, and `SampleNavHost` resets the chrome to `Default` on every destination
change, so Search does not restore it on disposal. The field sends SAGE's `SearchQueryEntered` and
`SearchClearClicked`; `SearchViewModel` waits 300 ms after typing and ignores queries shorter than
three characters, matching the other SAGE apps.
A settled query then runs two independent repository calls. `DemoSearchRepository` seeds each
call from the trimmed, lowercased query plus a list/grid salt, so the same query always produces
the same rows and `generated://` artwork, while each call independently picks a random count from
one to twenty results. The list call maps to `ImageNameCaptionListModel` rows and the grid call
to `GridImageListModel` cells; `SearchState` renders them as two headed sections, with per-section
loading and error rows. If both calls come back empty, the screen shows its no-results state.

The renderer suite is ported from Chipbox's `:cbox:common:ui:components:api` and keeps its shape:
one module that re-exports SAGE's `ListModel` types and owns every non-Chipbox-specific renderer,
plus `subs/` building blocks and `@Preview` harnesses. Accessibility labels are a module-owned
`ComponentStringId : SageStringId`; both composition roots install their graph's `StringProvider`
into `LocalComponentStringProvider`, so the renderers never touch `R.string`.

### Navigation

Navigation uses Voyager's `TabNavigator` (from SAGE's catalog). `SampleNavHost` owns the tabbed
shell:

- **Destinations** are in `SampleDestination`, which implements SAGE's `RouteDescriptor`.
- **Home, Browse and Search are tabs** (`SampleTabs.kt`), each owning a retained inner
  `Navigator` and a stable root screen. The `NavigationBar` on compact windows and the
  `NavigationRail` at 600 dp and wider highlight `tabNavigator.current`, so a pushed screen
  keeps its owning tab selected. Switching tabs pops the outgoing tab to its root (`popAll`),
  discarding that tab's pushed screens, while the tab you return to keeps its root view model
  and UI state (including the Search query and scroll position).
- **Back is one policy** (`backActionFor`): a pushed screen pops inside the active tab, Search
  at its root returns Home, and Home/Browse roots fall through to the host (Android finishes
  the activity; desktop has nothing above the shell). The top-bar arrow, Android system Back,
  and `SageEvent.NavigateBack` all run it. Desktop key and mouse Back inputs will use the same
  seam (`SampleSystemBack`).
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
  `ShowTopLevel` (select that tab, clearing the outgoing tab's stack) and any other route to
  `Push` onto the active tab (Settings included), so a feature can reach Settings the same way
  the hamburger does. `NavigateBack` runs the shared Back policy, and unknown routes or event
  types are ignored.
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
SAGE's `common` UI modules and Voyager directly; the app-owned port lives in `apps/jvm` because
`:apps:android` is Android-only. Four platform seams differ:

- **Text:** there are no Android resources. `SampleStrings` holds the same values as
  `apps/android/src/main/res/values/strings.xml`, and `SampleStringProvider` maps feature `SageStringId`s
  to them — the desktop twin of `SampleBindings`.
- **Theme and font:** both composition roots wrap `MaterialTheme` with `lightColorScheme` /
  `darkColorScheme`, selected by the persisted `ThemeMode` (light, dark, or follow the system),
  and `sampleTypography(fontFamily)` from the shared component module. `FontChoice` selects
  `FontFamily.Default` or Inter; each app loads its own copies of the Inter OTFs (Android
  `res/font`, desktop classpath `fonts/`). Both choices live in `SampleSettingsManager`, outside
  any screen's view model.
- **View model stores:** Voyager supplies per-screen `ViewModelStoreOwner`s only on Android.
  `WithPerScreenViewModelStore` hangs a store off a Voyager `ScreenModel`, so a screen keeps its
  view model while it stays in the back stack and clears it when popped. Each tab's inner navigator
  is retained by `TabNavigator`, so a tab root's store survives switching tabs here exactly as it
  does on Android.
- **Dispatchers and storage:** `JvmSampleBindings` provides `SageDispatchers` with
  `Dispatchers.Main` backed by `kotlinx-coroutines-swing` (the AWT event queue), plus the JVM
  `Hatchet`, analytics, `AppInfo`, and a properties-file `Storage` implementation.

The Metro graph is `JvmSampleAppGraph`; `Main.kt` builds it once and installs its
`metroViewModelFactory` above the window, exactly as `MainActivity` does on Android.

## Adding a feature

1. Create `features/<name>` with the `sage.jvm` + `sage.di` convention, and depend on
   `projects.core.viewmodel` plus the SAGE modules it needs.
2. Add `:features:<name>` to `settings.gradle.kts` and `implementation(projects.features.<name>)`
   in `apps/android/build.gradle.kts`.
3. Write a `<Name>State : ListState` and a `<Name>ViewModel : SampleListViewModel<<Name>State>`,
   using a `<Name>StringId : SageStringId` enum for text. Annotate the view model with
   `@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())` + `@ViewModelKey`, and
   send `SageAction.InitNoArgs` from `init`.
4. Map the string ids in `SampleBindings`; no graph accessor is needed for the view model.
5. To make it reachable, add a `SampleDestination`, a `SampleScreen` (resolving it with
   `metroViewModel`), and a `screenFor` branch in `SampleTabs.kt` — plus a nav label in
   `strings.xml` and a `SampleTab` if it becomes a top-level tab.
