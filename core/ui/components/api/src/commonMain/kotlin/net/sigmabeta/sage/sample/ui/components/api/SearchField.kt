package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import net.sigmabeta.sage.ui.Icon as SageIcon
import net.sigmabeta.sage.ui.vector

private val SearchFieldShape = RoundedCornerShape(32.dp)
private val SearchFieldElevation = 4.dp
private const val HINT_ALPHA = 0.5f

/**
 * The app-owned search box shared by both composition roots.
 *
 * It is deliberately not a SAGE `ListModel`: a text field is chrome, not list content, so the
 * screen overlays it on the grid, like the other SAGE apps. The caller owns the text and the
 * semantics strings, which keeps feature text flowing through the app's `StringProvider` mapping
 * rather than leaking `R.string` into this shared module.
 *
 * The leading back action is always present and is a standard 48 dp icon button, so the purple
 * container keeps the same height whether the field is empty or has text. The field requests
 * focus when it first enters composition; under `@Preview`/inspection the request is skipped so
 * layoutlib does not try to raise a real IME.
 */
@Composable
fun SearchField(
    text: String,
    hint: String,
    clearContentDescription: String,
    backContentDescription: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = SearchFieldShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = SearchFieldElevation, shape = SearchFieldShape),
    ) {
        val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        val textStyle = MaterialTheme.typography.titleMedium.copy(color = contentColor)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = SageIcon.Back.vector(),
                    contentDescription = backContentDescription,
                    tint = contentColor,
                )
            }

            val focusRequester = remember { FocusRequester() }
            val skipFocus = LocalInspectionMode.current
            LaunchedEffect(Unit) {
                if (!skipFocus) {
                    focusRequester.requestFocus()
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(horizontal = 8.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(contentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                if (text.isEmpty()) {
                    Text(
                        text = hint,
                        style = textStyle.copy(
                            color = contentColor.copy(alpha = HINT_ALPHA),
                        ),
                    )
                }
            }

            AnimatedVisibility(visible = text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = SageIcon.Clear.vector(),
                        contentDescription = clearContentDescription,
                        tint = contentColor,
                    )
                }
            }
        }
    }
}
