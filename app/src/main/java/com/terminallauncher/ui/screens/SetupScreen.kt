package com.terminallauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terminallauncher.ui.theme.Background
import com.terminallauncher.ui.theme.BorderSubtle
import com.terminallauncher.ui.theme.Monospace
import com.terminallauncher.ui.theme.TextDim
import com.terminallauncher.ui.theme.TextSecondary

@Composable
fun SetupScreen(
    onRequestUsageAccess: () -> Unit,
    onRequestDefaultLauncher: () -> Unit,
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
            text = "SETUP  $step/2",
            color = TextDim,
            fontFamily = Monospace,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (step) {
            1 -> UsageAccessStep(
                hasPermission = hasUsagePermission,
                onRequest = onRequestUsageAccess,
                onNext = { step = 2 }
            )
            2 -> DefaultLauncherStep(
                onRequest = onRequestDefaultLauncher
            )
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

