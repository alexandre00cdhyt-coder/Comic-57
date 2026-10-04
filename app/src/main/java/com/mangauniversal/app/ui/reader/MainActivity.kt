@Composable
fun LibraryPlaceholder() {
    val context = LocalContext.current
    var comics by remember { mutableStateOf<List<Comic>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        comics = withContext(Dispatchers.IO) {
            val scanner = ComicScanner(context)
            scanner.scanAll()
        }
        isLoading = false
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            when {
                isLoading -> {
                    Text(
                        text = "Escaneando almacenamiento...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
                comics == null -> {
                    Text(
                        text = "Error al escanear",
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                }
                comics!!.isEmpty() -> {
                    Text(
                        text = "No se encontraron cómics",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Prueba a copiar archivos CBZ, ZIP o PDF al teléfono",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Text(
                        text = "✅ Encontrados ${comics!!.size} cómics",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    comics!!.take(10).forEach { comic ->
                        Text(
                            text = "• ${comic.title} (${comic.type})",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (comics!!.size > 10) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "... y ${comics!!.size - 10} más",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
