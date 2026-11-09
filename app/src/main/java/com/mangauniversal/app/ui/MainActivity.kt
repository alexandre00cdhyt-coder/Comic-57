@Composable
fun MainTabsScreen(
    currentTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    folders: List<FolderConfig>,
    hasPermission: Boolean,
    onOpenRecientes: () -> Unit,
    onOpenStorage: (String) -> Unit,
    onOpenFavoriteFolder: (String) -> Unit,
    onAddFolder: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinueReading: (Comic) -> Unit,
    onOpenComicFromTab: (Comic) -> Unit,
    onOpenBookmark: (Comic, Int) -> Unit
) {
    // Estados para los nuevos botones
    var isEditMode by remember { mutableStateOf(false) }
    var viewModeState by remember { mutableStateOf(ViewMode.LIST_THUMB) }

    Column(modifier = Modifier.fillMaxSize()) { // Fondo transparente para que se vea la galería
        TopBar(
            currentTab = currentTab,
            onTabChange = onTabChange,
            onOpenSettings = onOpenSettings
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentTab) {
                is AppTab.Storage -> StorageExplorerScreen(
                    favoriteFolders = folders.filter { it.isFavorite }.map { it.path },
                    onOpenRecientes = onOpenRecientes,
                    onOpenStorage = onOpenStorage,
                    onOpenFavoriteFolder = onOpenFavoriteFolder,
                    onAddFolder = onAddFolder,
                    onOpenHistory = onOpenHistory,
                    onContinueReading = onContinueReading
                )
                is AppTab.Directory -> DirectoryTab(
                    hasPermission = hasPermission,
                    onOpenStorage = onOpenStorage
                )
                is AppTab.History -> HistoryScreenContent(
                    onOpenComic = { comic, _ -> onOpenComicFromTab(comic) }
                )
                is AppTab.Bookmark -> BookmarkScreen(
                    onOpenBookmark = onOpenBookmark
                )
            }
        }

        BottomBar(
            currentTab = currentTab,
            onTabChange = onTabChange,
            onOpenSettings = onOpenSettings,
            onEditClick = { isEditMode = !isEditMode },
            onViewClick = { 
                // Rota entre las vistas
                viewModeState = when (viewModeState) {
                    ViewMode.LIST_THUMB -> ViewMode.GRID_2
                    ViewMode.GRID_2 -> ViewMode.GRID_3
                    ViewMode.GRID_3 -> ViewMode.GRID_4
                    ViewMode.GRID_4 -> ViewMode.GRID_6
                    else -> ViewMode.LIST_THUMB
                }
            }
        )
    }
}

@Composable
fun TopBar(
    currentTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F3A4D).copy(alpha = 0.9f)) // Un poco transparente
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
            }
            Text(
                text = "0",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { }) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Más", tint = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16293A).copy(alpha = 0.9f)) // Un poco transparente
        ) {
            TabItem("Storage", currentTab is AppTab.Storage) { onTabChange(AppTab.Storage) }
            TabItem("Directory", currentTab is AppTab.Directory) { onTabChange(AppTab.Directory) }
            TabItem("History", currentTab is AppTab.History) { onTabChange(AppTab.History) }
            TabItem("Bookmark", currentTab is AppTab.Bookmark) { onTabChange(AppTab.Bookmark) }
        }
    }
}

@Composable
private fun RowScope.TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
        if (selected) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .background(Color(0xFF4A90E2))
            )
        }
    }
}

@Composable
fun BottomBar(
    currentTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    onOpenSettings: () -> Unit,
    onEditClick: () -> Unit,
    onViewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16293A).copy(alpha = 0.9f))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomBarItem(Icons.Default.Edit, "Edit") { onEditClick() }
        BottomBarItem(Icons.Default.History, "Recent") { onTabChange(AppTab.History) }
        BottomBarItem(Icons.Default.GridView, "View") { onViewClick() }
        BottomBarItem(Icons.Default.Settings, "Settings") { onOpenSettings() }
    }
}

@Composable
private fun BottomBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun DirectoryTab(
    hasPermission: Boolean,
    onOpenStorage: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Directorio", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Explora tus carpetas de cómics",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                onOpenStorage(Environment.getExternalStorageDirectory().absolutePath)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
        ) {
            Text("Abrir almacenamiento", color = Color.White)
        }
    }
}
