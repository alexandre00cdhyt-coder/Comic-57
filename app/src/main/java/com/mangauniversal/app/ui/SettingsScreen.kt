package com.mangauniversal.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mangauniversal.app.data.settings.AppSettings
import com.mangauniversal.app.data.settings.BackgroundConfig
import com.mangauniversal.app.data.settings.BackgroundManager
import com.mangauniversal.app.data.settings.ThemePreferences
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ThemePreferences(context) }
    val bgManager = remember { BackgroundManager(context) }

    var settings by remember { mutableStateOf(AppSettings()) }
    var bgConfig by remember { mutableStateOf(BackgroundConfig()) }
    var showResetDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = bgManager.saveImageFromUri(uri)
                if (ok) {
                    bgManager.saveEnabled(true)
                }
                bgConfig = bgManager.load()
            }
        }
    }

    LaunchedEffect(Unit) {
        settings = prefs.load()
        bgConfig = bgManager.load()
    }

    BackHandler(enabled = true) { onBack() }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Resetear ajustes", color = Color.White, fontSize = 16.sp) },
            text = {
                Text(
                    "¿Restaurar todos los ajustes a sus valores por defecto?",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    scope.launch {
                        prefs.resetAll()
                        settings = prefs.load()
                    }
                }) {
                    Text("Resetear", color = Color(0xFFFF5555), fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar", color = Color.White, fontSize = 13.sp)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F3A4D))
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
            }
            Text(
                text = "Ajustes",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            SectionHeader("FONDO")

            SwitchRow(
                title = "Usar fondo de galería",
                subtitle = if (bgConfig.imagePath != null) "Imagen seleccionada" else "Sin imagen",
                checked = bgConfig.useBackground,
                onCheckedChange = {
                    scope.launch {
                        bgManager.saveEnabled(it)
                        bgConfig = bgManager.load()
                    }
                }
            )

            if (bgConfig.imagePath != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = File(bgConfig.imagePath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Imagen actual",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Toca para cambiar",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            TextRow(
                title = "Elegir imagen de galería",
                subtitle = "Selecciona una foto como fondo",
                onClick = { imagePicker.launch("image/*") }
            )

            if (bgConfig.imagePath != null) {
                SliderRow(
                    title = "Opacidad del fondo",
                    value = bgConfig.opacity,
                    valueText = "${(bgConfig.opacity * 100).toInt()}%",
                    onValueChange = {
                        scope.launch {
                            bgManager.saveOpacity(it)
                            bgConfig = bgManager.load()
                        }
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            SectionHeader("GENERAL")

            TextRow(
                title = "Resetear ajustes",
                subtitle = "Restaurar valores por defecto",
                onClick = { showResetDialog = true }
            )

            Spacer(Modifier.height(8.dp))

            SectionHeader("LIST VIEW")

            SwitchRow(
                title = "Mostrar barra de estado",
                subtitle = "Hora, batería, wifi",
                checked = settings.showStatusBar,
                onCheckedChange = {
                    settings = settings.copy(showStatusBar = it)
                    scope.launch { prefs.saveShowStatusBar(it) }
                }
            )

            SwitchRow(
                title = "Mostrar ruta de carpeta",
                subtitle = "Ruta completa arriba",
                checked = settings.showFolderPath,
                onCheckedChange = {
                    settings = settings.copy(showFolderPath = it)
                    scope.launch { prefs.saveShowFolderPath(it) }
                }
            )

            SwitchRow(
                title = "Reanudar página",
                subtitle = "Continúa donde lo dejaste",
                checked = settings.resumePage,
                onCheckedChange = {
                    settings = settings.copy(resumePage = it)
                    scope.launch { prefs.saveResumePage(it) }
                }
            )

            Spacer(Modifier.height(8.dp))

            SectionHeader("IMAGE VIEW")

            SwitchRow(
                title = "Pantalla completa",
                subtitle = "Ocultar barras del sistema",
                checked = settings.fullScreenImage,
                onCheckedChange = {
                    settings = settings.copy(fullScreenImage = it)
                    scope.launch { prefs.saveFullScreen(it) }
                }
            )

            Spacer(Modifier.height(8.dp))

            SectionHeader("LECTURA")

            ThemeSelector(
                currentTheme = settings.readerTheme,
                onSelect = {
                    settings = settings.copy(readerTheme = it)
                    scope.launch { prefs.saveReaderTheme(it) }
                }
            )

            SliderRow(
                title = "Filtro luz azul",
                value = settings.blueLightFilter,
                valueText = "${(settings.blueLightFilter * 100).toInt()}%",
                onValueChange = {
                    settings = settings.copy(blueLightFilter = it)
                    scope.launch { prefs.saveBlueLightFilter(it) }
                }
            )

            SliderRow(
                title = "Brillo de la app",
                value = settings.brightness,
                valueText = "${(settings.brightness * 100).toInt()}%",
                onValueChange = {
                    settings = settings.copy(brightness = it)
                    scope.launch { prefs.saveBrightness(it) }
                }
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFF4A90E2),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun TextRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4A90E2),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF2A2A2A)
            )
        )
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                color = Color(0xFFFF6B00),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF6B00),
                activeTrackColor = Color(0xFFFF6B00),
                inactiveTrackColor = Color(0xFF2A2A2A)
            )
        )
    }
}

@Composable
private fun ThemeSelector(
    currentTheme: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Tema de lectura",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeButton("OSCURO", "DARK", currentTheme, onSelect)
            ThemeButton("SEPIA", "SEPIA", currentTheme, onSelect)
            ThemeButton("BLANCO", "LIGHT", currentTheme, onSelect)
        }
    }
}

@Composable
private fun RowScope.ThemeButton(
    label: String,
    value: String,
    currentTheme: String,
    onSelect: (String) -> Unit
) {
    val selected = currentTheme == value
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFFF6B00) else Color(0xFF2A2A2A))
            .clickable { onSelect(value) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
