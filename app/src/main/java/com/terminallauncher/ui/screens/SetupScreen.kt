package com.terminallauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terminallauncher.ui.theme.Background
import com.terminallauncher.ui.theme.BorderSubtle
import com.terminallauncher.ui.theme.Monospace
import com.terminallauncher.ui.theme.TextDim
import com.terminallauncher.ui.theme.TextInput
import com.terminallauncher.ui.theme.TextPlaceholder
import com.terminallauncher.ui.theme.TextSecondary
import java.util.Calendar

@Composable
fun SetupScreen(
    onSaveBirthDate: (year: Int, month: Int) -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onSetWallpaperConfig: (home: Boolean, lock: Boolean) -> Unit,
    hasUsagePermission: Boolean
) {
    var step by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Step indicator
        Text(
            text = "SETUP  $step/4",
            color = TextDim,
            fontFamily = Monospace,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (step) {
            1 -> BirthDateStep(onNext = { year, month ->
                onSaveBirthDate(year, month)
                step = 2
            })
            2 -> UsageAccessStep(
                hasPermission = hasUsagePermission,
                onRequest = onRequestUsageAccess,
                onNext = { step = 3 }
            )
            3 -> WallpaperStep(
                onSetConfig = onSetWallpaperConfig,
                onNext = { step = 4 }
            )
            4 -> DefaultLauncherStep(
                onRequest = onRequestDefaultLauncher
            )
        }
    }
}

@Composable
private fun BirthDateStep(onNext: (year: Int, month: Int) -> Unit) {
    var yearText by remember { mutableStateOf("") }
    var monthText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    Text(
        text = "when were you born?",
        color = TextSecondary,
        fontFamily = Monospace,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UnderlineField(
            value = yearText,
            onValueChange = { yearText = it.take(4); error = null },
            placeholder = "yyyy",
            modifier = Modifier.width(80.dp)
        )

        Text(
            text = "/",
            color = TextPlaceholder,
            fontFamily = Monospace,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        UnderlineField(
            value = monthText,
            onValueChange = { monthText = it.take(2); error = null },
            placeholder = "mm",
            modifier = Modifier.width(50.dp)
        )
    }

    Text(
        text = "year / month",
        color = TextDim,
        fontFamily = Monospace,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 12.dp)
    )

    if (error != null) {
        Text(
            text = error!!,
            color = TextSecondary,
            fontFamily = Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }

    Spacer(modifier = Modifier.height(40.dp))

    SetupButton("continue →") {
        val year = yearText.toIntOrNull()
        val month = monthText.toIntOrNull()
        when {
            year == null || year < 1920 || year > currentYear ->
                error = "enter a valid year (1920-$currentYear)"
            month == null || month < 1 || month > 12 ->
                error = "enter a valid month (01-12)"
            else -> onNext(year, month)
        }
    }
}

@Composable
private fun UsageAccessStep(
    hasPermission: Boolean,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    Text(
        text = "allow usage access?",
        color = TextSecondary,
        fontFamily = Monospace,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "sorts your most-used apps\nto the top of search results",
        color = TextDim,
        fontFamily = Monospace,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(40.dp))

    if (hasPermission) {
        Text(
            text = "granted ✓",
            color = TextSecondary,
            fontFamily = Monospace,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        SetupButton("continue →", onClick = onNext)
    } else {
        SetupButton("open settings →", onClick = onRequest)
        Spacer(modifier = Modifier.height(16.dp))
        SetupButton("skip →", onClick = onNext)
    }
}

@Composable
private fun WallpaperStep(
    onSetConfig: (home: Boolean, lock: Boolean) -> Unit,
    onNext: () -> Unit
) {
    var home by remember { mutableStateOf(true) }
    var lock by remember { mutableStateOf(true) }

    Text(
        text = "set life grid as wallpaper?",
        color = TextSecondary,
        fontFamily = Monospace,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    SetupToggle("home screen", home) { home = !home }
    Spacer(modifier = Modifier.height(12.dp))
    SetupToggle("lock screen", lock) { lock = !lock }

    Spacer(modifier = Modifier.height(40.dp))

    SetupButton("continue →") {
        onSetConfig(home, lock)
        onNext()
    }

    Spacer(modifier = Modifier.height(12.dp))

    SetupButton("skip →") {
        onSetConfig(false, false)
        onNext()
    }
}

@Composable
private fun SetupToggle(label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = if (enabled) "[x]" else "[ ]",
            color = if (enabled) com.terminallauncher.ui.theme.CopperLived else TextDim,
            fontFamily = Monospace,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = TextSecondary, fontFamily = Monospace, fontSize = 14.sp)
    }
}

@Composable
private fun DefaultLauncherStep(
    onRequest: () -> Unit
) {
    Text(
        text = "set as default launcher?",
        color = TextSecondary,
        fontFamily = Monospace,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "replaces your current home screen\nwith the terminal launcher",
        color = TextDim,
        fontFamily = Monospace,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(40.dp))

    SetupButton("set as default →", onClick = onRequest)
}

@Composable
private fun SetupButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = TextSecondary,
            fontFamily = Monospace,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun UnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val underlineColor = TextPlaceholder

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = TextInput,
            fontFamily = Monospace,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(TextInput),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
            .drawBehind {
                drawLine(
                    color = underlineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 8.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = TextPlaceholder,
                        fontFamily = Monospace,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        }
    )
}
