# `:features:login`

The sign-in form: username, masked password, a Sign in button, and an inline message for each
failed outcome. The attempt goes through `:core:repository:api`'s `IdentityRepository`; this module
never navigates. The app's sign-in gate reacts to the repository's state, not to this screen.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
}
```

## Rows

`LoginState.toListItems` builds, in order:

1. Username — `TextFieldListModel` (`USERNAME_FIELD_ID`)
2. Password — `TextFieldListModel` (`PASSWORD_FIELD_ID`), masked, keyboard Done submits
3. The failure message, if any — `FormMessageListModel` with `FormMessageTone.ERROR`
4. Sign in — `ButtonListModel`; it reads "Signing in…" while an attempt is in flight
5. The demo-account hint — `FormMessageListModel` with `FormMessageTone.INFO`

The form models come from `:core:ui:components:models`. Sign in is enabled only when both fields
are non-empty and no attempt is in flight. Credentials are sent exactly as typed, without trimming.

## View model

`LoginViewModel : SampleListViewModel<LoginState>`. It is contributed with
`@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())` + `@ViewModelKey`, and
resolved with `metroViewModel<LoginViewModel>()`. The title is "Sign in" and it has no back.

- `TextFieldChanged` updates the matching field. Editing after a failure clears the message.
- `ButtonListModel` clicks and `TextFieldDone` on the password field submit. If an attempt is
  already in flight, a new submit does nothing.
- The attempt runs on the network dispatcher, so the Settings simulated delay applies.
  - `Accepted` emits `LoginEvent.SignedIn(user)` and resets the form. A new `formKey` clears
    both fields.
  - `Rejected`, `Throttled` and `Unreachable` each show their own message.
  - An exception thrown by `signIn` is shown as `Unreachable`.

## Strings

`SampleStringId.LOGIN_*`: the title, both field labels, the submit and submitting labels, the three
failure messages, and the demo-account hint.

## Dependencies

| Dependency | Why |
|---|---|
| `:core:viewmodel` (`api`) | Base class, scheduler |
| `:core:repository:api` (`api`) | `IdentityRepository`, `SignInOutcome`, `User` |
| `:core:ui:components:models` (`api`) | Text field, button and message rows |
| `:core:strings:api` | `SampleStringId` |
| `metrox-viewmodel` (`api`) | `@ViewModelKey` |
| SAGE `list`, `appcomm`, `analytics`, `logging`, `ui.components`, `ui.strings` | State/models, actions/events, analytics |

## Tests

`LoginViewModelTest` covers row order, button enablement, the submit triggers, the double-submit
guard, each outcome's message, the reset and `SignedIn` event after acceptance, clear-on-edit, and
mapping an exception to `Unreachable`. `LoginTestFixtures.kt` holds a scripted fake
`IdentityRepository`.

`:features:login:screenshot` records six hand-built states (empty, filled, submitting, rejected,
throttled, unreachable) across the shared device and theme set.
