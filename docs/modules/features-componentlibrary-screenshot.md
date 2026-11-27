# `:features:componentlibrary:screenshot`

Paparazzi goldens for the Component Library, one fixture per layout, each the default
`ComponentLibraryConfig` (default style, seed-0 content) wrapped in `ComponentLibraryHost`.

```kotlin
plugins {
    id("sagesample.screenshot")
}

dependencies {
    implementation(projects.features.componentlibrary)
}
```

| Preview | Fixture |
|---|---|
| `ComponentLibraryList` | LIST layout |
| `ComponentLibraryGrid` | GRID layout |
| `ComponentLibraryColumns` | COLUMNS (detail-style) layout |

The viewport shows the Controls section and the first showcase rows; the sample's form rows sit far
down the list and are covered by unit tests rather than the goldens.
