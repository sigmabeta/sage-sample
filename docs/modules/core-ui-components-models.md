# `:core:ui:components:models`

The sample's own `ListModel`s for forms: a text field, a button, and an inline message. Features
build these rows and `:core:ui:components:api` renders them. The module has no Compose, because
features must not depend on Compose.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
}
```

SAGE's `EditTextListModel` has its own submit/cancel pair and no masking, so it does not fit a
sign-in form. SAGE's `ListModel` and `SageAction` are open, so these types extend them without a
SAGE change.

## Types

All in `net.sigmabeta.sage.sample.ui.components.models` (`FormListModels.kt`):

| Type | What it is |
|---|---|
| `TextFieldListModel(id, label, text, masked, enabled, submitOnDone, resetKey)` | One labelled field. `masked` hides the text; `submitOnDone` makes IME Done send `TextFieldDone`. Bumping `resetKey` makes the renderer drop its local text and take `text` again. |
| `ButtonListModel(label, clickAction, enabled, dataId)` | A full-width button that sends `clickAction`. `dataId` is required: a list id never derives from the label. |
| `FormMessageListModel(text, tone, dataId)` | Inline text; `FormMessageTone` is `INFO` or `ERROR`. `dataId` is required, as for the button. |
| `TextFieldChanged(id, text)` | `SageAction` sent on each edit. |
| `TextFieldDone(id)` | `SageAction` sent on IME Done when `submitOnDone` is set. |

Labels and text are `SageStringId`s, so the strings module owns the words.

## Dependencies

| Dependency | Why |
|---|---|
| `sage.common.ui.components` (`api`) | `ListModel`, the base these rows extend |
| `sage.common.appcomm` (`api`) | `SageAction` |

## Tests

None. The behaviour is in the feature's view model tests and the Login screenshot goldens.
