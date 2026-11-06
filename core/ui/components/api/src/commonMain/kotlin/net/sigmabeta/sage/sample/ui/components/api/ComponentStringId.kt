package net.sigmabeta.sage.sample.ui.components.api

import net.sigmabeta.sage.ui.SageStringId

/**
 * Accessibility text owned by the component renderers.
 *
 * These are labels for platform semantics (`onClickLabel`, `stateDescription`) rather than
 * visible content, but they still follow the sample's text rule: a UI module emits a
 * [SageStringId] and each app maps it to real text. The Android mapping lives in
 * `SampleBindings`, the desktop one in `SampleStringProvider`.
 */
enum class ComponentStringId : SageStringId {
    ACCY_OCL_CHECKBOX,
    ACCY_OCL_DROPDOWN,
    ACCY_OCL_RATING,
    ACCY_OCL_SINGLE_LINE,
    ACCY_OCL_VALUE,
    ACCY_ST_DESC_CHECKED,
    ACCY_ST_DESC_LOADING,
    ACCY_ST_DESC_UNCHECKED,
}
