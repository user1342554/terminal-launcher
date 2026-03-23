package com.terminallauncher.ui.components

import android.view.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terminallauncher.data.SearchResult
import com.terminallauncher.ui.theme.Background
import com.terminallauncher.ui.theme.Monospace
import com.terminallauncher.ui.theme.SelectionBg
import com.terminallauncher.ui.theme.TextDim
import com.terminallauncher.ui.theme.TextInput
import com.terminallauncher.ui.theme.TextPlaceholder

@Composable
fun TerminalOverlay(
    visible: Boolean,
    query: String,
    results: List<SearchResult>,
    selectedIndex: Int,
    onQueryChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onLaunch: () -> Unit,
    onLaunchIndex: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        view.post {
            view.rootView.windowInsetsController?.show(WindowInsets.Type.ime())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background.copy(alpha = 0.85f))
            .imePadding()
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$ ",
                    color = TextPlaceholder,
                    fontFamily = Monospace,
                    fontSize = 16.sp
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextInput,
                        fontFamily = Monospace,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(TextInput),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onLaunch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = "type to search...",
                                    color = TextPlaceholder,
                                    fontFamily = Monospace,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            val display = results.take(5)
            if (display.isNotEmpty()) {
                display.forEachIndexed { index, result ->
                    ResultRow(result, index == selectedIndex) { onLaunchIndex(index) }
                }
            } else if (query.isNotEmpty()) {
                Text(
                    text = "no results",
                    color = TextDim,
                    fontFamily = Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult, isSelected: Boolean, onClick: () -> Unit) {
    val label = result.app.label.lowercase()
    val matched = remember(result) { result.matchedIndices.toSet() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (isSelected) Modifier.background(SelectionBg) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (isSelected) "▸ " else "  ",
            color = TextPlaceholder,
            fontFamily = Monospace,
            fontSize = 14.sp
        )
        Text(
            text = buildAnnotatedString {
                val hi = if (isSelected) TextInput else TextInput.copy(alpha = 0.7f)
                val lo = if (isSelected) TextInput.copy(alpha = 0.5f) else TextDim.copy(alpha = 0.5f)
                for (i in label.indices) {
                    withStyle(SpanStyle(color = if (i in matched) hi else lo)) { append(label[i]) }
                }
            },
            fontFamily = Monospace,
            fontSize = 16.sp
        )
    }
}
