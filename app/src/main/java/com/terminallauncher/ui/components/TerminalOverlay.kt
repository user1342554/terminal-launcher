package com.terminallauncher.ui.components

import android.view.WindowInsets
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terminallauncher.HistoryEntry
import com.terminallauncher.data.SearchResult
import com.terminallauncher.data.TerminalOutput
import com.terminallauncher.ui.theme.Background
import com.terminallauncher.ui.theme.CopperLived
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
    isCommandMode: Boolean,
    history: List<HistoryEntry>,
    currentPath: String,
    onQueryChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSubmit: () -> Unit,
    onLaunchIndex: (Int) -> Unit,
    onDismiss: () -> Unit,
    onTabComplete: () -> Unit = {},
    showAppPicker: Boolean = false
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
            .background(Background.copy(alpha = 0.95f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp)
        ) {
            // === HISTORY (above input, last 3 commands) ===
            history.takeLast(3).forEach { entry ->
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = CopperLived)) { append("$ ") }
                        withStyle(SpanStyle(color = TextInput)) { append(entry.command) }
                    },
                    fontFamily = Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                entry.output.take(15).forEach { line ->
                    when (line) {
                        is TerminalOutput.TextLine -> Text(
                            text = line.text,
                            color = if (line.dim) TextDim else TextInput.copy(alpha = 0.8f),
                            fontFamily = Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 1.dp, bottom = 1.dp)
                        )
                        is TerminalOutput.Error -> Text(
                            text = line.text,
                            color = Color(0xFFFF6B6B),
                            fontFamily = Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 1.dp, bottom = 1.dp)
                        )
                        is TerminalOutput.SelectableItem -> Text(
                            text = line.label,
                            color = CopperLived,
                            fontFamily = Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 1.dp, bottom = 1.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // === INPUT LINE ===
            // Blinking cursor
            val blink = rememberInfiniteTransition(label = "cursor")
            val cursorAlpha by blink.animateFloat(
                initialValue = 1f, targetValue = 0f,
                animationSpec = infiniteRepeatable(tween(530), RepeatMode.Reverse),
                label = "cursorAlpha"
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$ ",
                    color = if (isCommandMode) CopperLived else TextPlaceholder,
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
                    cursorBrush = SolidColor(TextInput.copy(alpha = cursorAlpha)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = if (showAppPicker) "pick app for swipe shortcut..." else "search or command...",
                                    color = if (showAppPicker) CopperLived.copy(alpha = 0.6f) else TextPlaceholder,
                                    fontFamily = Monospace,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                // Tab button
                if (isCommandMode) {
                    Text(
                        text = "TAB",
                        color = TextDim,
                        fontFamily = Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable(onClick = onTabComplete)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // === SEARCH RESULTS (below input, growing down) ===
            if (!isCommandMode && results.isNotEmpty()) {
                results.take(5).forEachIndexed { index, result ->
                    ResultRow(result, index == selectedIndex) { onLaunchIndex(index) }
                }
            } else if (!isCommandMode && query.isNotEmpty() && results.isEmpty()) {
                Text(
                    text = "no results",
                    color = TextDim,
                    fontFamily = Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (isCommandMode && query.isNotEmpty()) {
                Text(
                    text = "↵ run command",
                    color = TextDim,
                    fontFamily = Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
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
