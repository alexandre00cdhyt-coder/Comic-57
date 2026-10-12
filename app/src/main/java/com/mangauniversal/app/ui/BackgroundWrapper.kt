package com.mangauniversal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.mangauniversal.app.data.settings.BackgroundConfig
import com.mangauniversal.app.data.settings.BackgroundManager
import java.io.File

@Composable
fun BackgroundWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { BackgroundManager(context) }

    var config by remember { mutableStateOf(BackgroundConfig()) }

    LaunchedEffect(Unit) {
        config = manager.load()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E))) {
        if (config.useBackground && config.imagePath != null) {
            AsyncImage(
                model = File(config.imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0E0E0E).copy(alpha = 1f - config.opacity))
            )
        }
        content()
    }
}
