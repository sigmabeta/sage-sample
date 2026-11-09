# `:features:home`

Home is module-driven: a pluggable row contract, a per-screen
plan built from Metro-registered definitions, and one independently-loading slot per row.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
}
```

## The row contract

| Type | Role |
|---|---|
| `HomeModule` | One row: stable `id`, `priority`, `showHeader`, and a `Flow<LCE<HomeModuleSection>>`. |
| `HomeModuleSection` | Resolved row: `title` + pre-built `ImmutableList<ListModel>`. |
| `HomeModuleDefinition` | Registered row type: `typeId`, `priority`, `create(instanceNumber, dataIdBase)`. |
| `HomeModuleFactory` | Turns the graph's definitions into one plan. Stable mode: one instance per definition in priority order. Randomized mode: 2–10 instances, every definition represented, repeats shuffled. Each slot gets a unique instance number and a 10,000,000-wide data-id range. |
| `HomeModuleBindings` | `@ContributesTo(AppScope)` interface with `@Multibinds Set<HomeModuleDefinition>` + one `@Binds @IntoSet` per row. Adding a row is one binding line. |

The factory is injected per screen (`@Inject`, not `@SingleIn`), so popping and re-opening Home
rebuilds the plan. Ties in priority break on `typeId` for a deterministic stable plan.

## State and rendering

`HomeState(sections: ImmutableList<HomeSectionState>)` keeps one slot per module, in priority order.
Each `HomeSectionState` owns its `LCE`, `showHeader`, and a `hasEmitted` flag.

- Per-row loading uses `LoadingType.COVER`, `loadingHorizScrollable = true`, and a per-module
  operation name (`home.<id>.load`), so one module's failure never replaces its neighbours.
- A single-item row renders full width; multi-item rows are wrapped in
  `HorizontalScrollerListModel`; an empty row renders nothing (no bare heading).
- When every module has emitted and all failed, the screen renders one `HOME_ALL_ERROR` row.
- When every module has emitted and all resolved to no items, it renders one `HOME_EMPTY` row.
  `hasEmitted` distinguishes "plan not started yet" from "genuinely empty", so a plan rebuild never
  flashes the empty state.

## View model

`HomeViewModel`:

- Builds the initial plan from `homeShowsSameContent.value`, then collects every module's flow in
  parallel and patches only that module's slot.
- Observes `homeShowsSameContent` and `demoOutcome`. A real change cancels the current collectors,
  rebuilds the plan, and starts over (the tab root is retained, so the screen is not recreated).
- A `planGeneration` counter ignores a stale update queued by a cancelled collector.
- `HomeAction.ItemClicked(contentId)` emits `NavigateTo(contentDetailRoute(contentId))`.
- `homeContentId(moduleId, itemId) = "home:<moduleId>:<itemId>"` keeps repeated module types routed
  apart.

## Shipped modules

| Module | Behaviour |
|---|---|
| `BigImageModule` (`big_image`, priority 0) | One item per UTC epoch day, chosen by count + indexed fetch (`dailyIndex`). Renders one width-capped `GridImageListModel` (`maxWidthDp = 400`), aspect 3:4. Hides itself on an empty catalog. |
| `GridImagesModule` (`grid_images`, priority 1) | Asks the repository for 10 items and hides itself below 3, so a nearly empty row never flashes. Renders `GridImageListModel` cells. |

Both seed content and artwork with the module instance `id`, so two instances of the same type show
different titles and art, and revisiting one reproduces it.

## Data

`HomeRepository` (`:core:repository:api`) is shaped for the two modules: `suspend itemCount(seed)`,
`suspend itemAt(seed, index)`, and `gridImages(seed, limit): Flow<LCE<List<HomeItem>>>`. Neither
call loads the whole catalog to filter in memory.

`DemoHomeRepository` (`:core:repository:demo`) generates an 18-item catalog from the seed and reads
`DemoOutcome` **per call** (`CONTENT` / `EMPTY` / `ERROR`), so the Home empty and all-error states
are reachable from Settings without a restart. The browser binds `RemoteHomeRepository` instead and
the same interface is answered by `apps:server`.

## Dependencies

| Dependency | Why |
|---|---|
| `:core:viewmodel` (`api`) | Base class |
| `:core:repository:api` (`api`) | Repository seams, constants and wire contract |
| `:core:models:api` | `HomeItem` + the Content-detail route codec |
| `:core:strings:api` | `SampleStringId` vocabulary |
| `:core:bitmaps` | `generated://` URLs |
| `metrox-viewmodel` (`api`) | `@ViewModelKey` |
| `kotlinx-collections-immutable` | Section item lists |
| SAGE `list`, `appcomm`, `analytics`, `logging`, `ui.components`, `ui.strings` | State/models, actions, analytics, `StringProvider` |

## Tests

`HomeModuleFactoryTest` (stable/randomized plans, bounds, unique ids, priority order),
`HomeStateTest` (per-row loading/error/content, all-error, empty + `hasEmitted`),
`HomeViewModelTest` (parallel collection, plan replacement, generation guard, navigation),
`HomeModulesTest` (daily pick and row hiding), `DemoHomeRepositoryTest` (determinism, outcomes; the module has a `jvmTest` dependency on `:core:repository:demo`).
