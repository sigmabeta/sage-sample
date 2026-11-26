# `:features:login:screenshot`

Paparazzi visual-regression companion for Login. Each fixture is a hand-built `LoginState`, so the
goldens never wait on the identity seam or the simulated delay.

```kotlin
plugins {
    id("sagesample.screenshot")
}

dependencies {
    implementation(projects.features.login)
}
```

## States

| Preview | Fixture |
|---|---|
| `LoginEmpty` | Empty fields, Sign in disabled, the Demo account hint. |
| `LoginFilled` | `demo` / `sage` typed (password masked), Sign in enabled. |
| `LoginSubmitting` | Filled form mid-attempt: the button reads "Signing in…" and is disabled. |
| `LoginRejected` | Filled form with the wrong-credentials message above Sign in. |
| `LoginThrottled` | Filled form with the too-many-attempts message. |
| `LoginUnreachable` | Filled form with the can't-reach-the-server message. |

## Files

| File | What it is |
|---|---|
| `src/main/java/.../Login.kt` | The six `@DevicePreviews` composables and their `LoginState` fixtures. |
| `src/test/java/.../LoginScreenshots.kt` | Parameterized Paparazzi test over `INTERESTING_DEVICES`. |
| `src/test/snapshots/images/` | The LFS goldens (6 states × 12 device/theme configs). |

## Module facts

- **Plugin:** `sagesample.screenshot` (sage.android + sage.compose.android + Paparazzi).
- **Targets:** Android only. **Source sets:** `src/main/java` (fixtures) + `src/test/java` (tests).
- **Dependencies:** `:features:login`; the convention adds `:core:ui:previews`,
  `:core:strings:real`, SAGE `appcomm`/`list`.
- **Text/theme:** both come from the shared modules — `SampleStringProvider` and `SampleTheme`.
