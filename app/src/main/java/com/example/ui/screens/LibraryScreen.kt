package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.MediaType
import com.example.data.model.PlaylistEntity
import com.example.data.model.SortOption
import com.example.data.model.TrackEntity
import com.example.ui.MainViewModel
import com.example.util.PermissionUtils
import kotlinx.coroutines.launch

enum class LibraryViewMode(val label: String, val icon: ImageVector) {
    LIST("List View", Icons.Default.ViewList),
    GRID("Grid View", Icons.Default.GridView),
    CARD("Card View", Icons.Default.ViewAgenda)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    tracks: List<TrackEntity>,
    playlists: List<PlaylistEntity>,
    onOpenLyricsDialog: (TrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val prefs = remember { context.getSharedPreferences("omniplay_prefs", Context.MODE_PRIVATE) }
    var viewMode by remember {
        mutableStateOf(
            try {
                LibraryViewMode.valueOf(prefs.getString("library_view_mode", LibraryViewMode.LIST.name) ?: LibraryViewMode.LIST.name)
            } catch (e: Exception) {
                LibraryViewMode.LIST
            }
        )
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var showViewModeMenu by remember { mutableStateOf(false) }
    var selectedTrackForMenu by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToDelete by remember { mutableStateOf<TrackEntity?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    var hasPermission by remember { mutableStateOf(PermissionUtils.hasMediaPermissions(context)) }
    val isScanning by viewModel.isScanning.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        hasPermission = granted || PermissionUtils.hasMediaPermissions(context)
        if (granted || hasPermission) {
            viewModel.scanDeviceMedia(context)
        }
    }

    LaunchedEffect(Unit) {
        if (PermissionUtils.hasMediaPermissions(context)) {
            hasPermission = true
            viewModel.scanDeviceMedia(context)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importLocalFile(it, context) }
    }

    fun updateViewMode(mode: LibraryViewMode) {
        viewMode = mode
        prefs.edit().putString("library_view_mode", mode.name).apply()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize().testTag("library_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Media Library",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = "Import Media")
                    }

                    IconButton(
                        onClick = {
                            if (hasPermission) {
                                viewModel.scanDeviceMedia(context)
                            } else {
                                permissionLauncher.launch(PermissionUtils.getRequiredMediaPermissions())
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan Media")
                    }

                    // View Mode Switcher
                    Box {
                        IconButton(onClick = { showViewModeMenu = !showViewModeMenu }) {
                            Icon(imageVector = viewMode.icon, contentDescription = "View Layout")
                        }

                        DropdownMenu(
                            expanded = showViewModeMenu,
                            onDismissRequest = { showViewModeMenu = false }
                        ) {
                            LibraryViewMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    leadingIcon = { Icon(mode.icon, contentDescription = null) },
                                    onClick = {
                                        updateViewMode(mode)
                                        showViewModeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Sort Menu
                    Box {
                        IconButton(onClick = { showSortMenu = !showSortMenu }) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("Sort by ${option.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}") },
                                    onClick = {
                                        viewModel.librarySortOption.value = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!hasPermission) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Storage Permission Needed",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Allow app to automatically discover audio & video files saved on your device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(PermissionUtils.getRequiredMediaPermissions())
                                }
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }

            // MediaType Segment Filter Pills (All, Audio, Video)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                val selectedTab = viewModel.selectedMediaTypeTab.value
                FilterChip(
                    selected = selectedTab == null,
                    onClick = { viewModel.selectedMediaTypeTab.value = null },
                    label = { Text("All Media") }
                )
                FilterChip(
                    selected = selectedTab == MediaType.AUDIO,
                    onClick = { viewModel.selectedMediaTypeTab.value = MediaType.AUDIO },
                    label = { Text("Audio Only") }
                )
                FilterChip(
                    selected = selectedTab == MediaType.VIDEO,
                    onClick = { viewModel.selectedMediaTypeTab.value = MediaType.VIDEO },
                    label = { Text("Video Only") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .height(64.dp)
                                .width(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your media library is empty",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { filePickerLauncher.launch("*/*") }) {
                            Text("Upload Local Audio/Video File")
                        }
                    }
                }
            } else {
                when (viewMode) {
                    LibraryViewMode.LIST -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tracks, key = { it.id }) { track ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.removeTrackFromLibrary(context, track)
                                            coroutineScope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Removed '${track.title}'",
                                                    actionLabel = "UNDO",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.restoreTrack(context, track)
                                                }
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color = if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                                            MaterialTheme.colorScheme.errorContainer
                                        } else {
                                            Color.Transparent
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(color)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    },
                                    content = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            TrackListItemRow(
                                                track = track,
                                                onClick = { viewModel.playTrack(track, tracks) },
                                                onToggleFavorite = { viewModel.toggleFavorite(track) },
                                                modifier = Modifier.weight(1f)
                                            )

                                            TrackMoreButton(
                                                track = track,
                                                selectedTrack = selectedTrackForMenu,
                                                onSelect = { selectedTrackForMenu = it },
                                                onAddToPlaylist = { showAddToPlaylistDialog = true },
                                                onOpenLyrics = { onOpenLyricsDialog(track) },
                                                onDeleteRequest = { trackToDelete = track }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    LibraryViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tracks, key = { it.id }) { track ->
                                TrackGridCardItem(
                                    track = track,
                                    onClick = { viewModel.playTrack(track, tracks) },
                                    onMoreClick = { selectedTrackForMenu = track },
                                    selectedTrack = selectedTrackForMenu,
                                    onAddToPlaylist = { showAddToPlaylistDialog = true },
                                    onOpenLyrics = { onOpenLyricsDialog(track) },
                                    onDeleteRequest = { trackToDelete = track }
                                )
                            }
                        }
                    }

                    LibraryViewMode.CARD -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tracks, key = { it.id }) { track ->
                                TrackLargeCardItem(
                                    track = track,
                                    onClick = { viewModel.playTrack(track, tracks) },
                                    onToggleFavorite = { viewModel.toggleFavorite(track) },
                                    onMoreClick = { selectedTrackForMenu = track },
                                    selectedTrack = selectedTrackForMenu,
                                    onAddToPlaylist = { showAddToPlaylistDialog = true },
                                    onOpenLyrics = { onOpenLyricsDialog(track) },
                                    onDeleteRequest = { trackToDelete = track }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    trackToDelete?.let { track ->
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = { Text("Delete '${track.title}'") },
            text = {
                Text("Choose an option:\n\n• Remove from Library: Removes track from OmniPlay library, keeping the file on your device.\n• Delete from Device: Deletes the physical file permanently from storage.")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.removeTrackFromLibrary(context, track)
                            trackToDelete = null
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Removed '${track.title}'",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreTrack(context, track)
                                }
                            }
                        }
                    ) {
                        Text("Remove from Library")
                    }

                    Button(
                        onClick = {
                            viewModel.deleteTrackFromDevice(context, track)
                            trackToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete from Device")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { trackToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog && selectedTrackForMenu != null) {
        val targetTrack = selectedTrackForMenu!!
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddToPlaylistDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Add to Playlist",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (playlists.isEmpty()) {
                        Text("No playlists found. Create one in the Playlists tab!")
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(playlists) { playlist ->
                                DropdownMenuItem(
                                    text = { Text(playlist.name) },
                                    onClick = {
                                        viewModel.addTrackToPlaylist(playlist.id, targetTrack.id)
                                        showAddToPlaylistDialog = false
                                        selectedTrackForMenu = null
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showAddToPlaylistDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun TrackListItemRow(
    track: TrackEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!track.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = "Track Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (track.mediaType == MediaType.VIDEO) Icons.Default.Videocam else Icons.Default.MusicNote,
                        contentDescription = "Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TrackMoreButton(
    track: TrackEntity,
    selectedTrack: TrackEntity?,
    onSelect: (TrackEntity?) -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenLyrics: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    Box {
        IconButton(onClick = { onSelect(track) }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
        }

        DropdownMenu(
            expanded = selectedTrack == track,
            onDismissRequest = { onSelect(null) }
        ) {
            DropdownMenuItem(
                text = { Text("Add to Playlist") },
                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                onClick = {
                    onAddToPlaylist()
                }
            )
            DropdownMenuItem(
                text = { Text(if (track.mediaType == MediaType.VIDEO) "Edit Subtitles" else "Edit Lyrics") },
                leadingIcon = { Icon(Icons.Default.Subtitles, contentDescription = null) },
                onClick = {
                    onOpenLyrics()
                    onSelect(null)
                }
            )
            DropdownMenuItem(
                text = { Text("Delete Track", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDeleteRequest()
                    onSelect(null)
                }
            )
        }
    }
}

@Composable
fun TrackGridCardItem(
    track: TrackEntity,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    selectedTrack: TrackEntity?,
    onAddToPlaylist: () -> Unit,
    onOpenLyrics: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (!track.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (track.mediaType == MediaType.VIDEO) Icons.Default.Videocam else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TrackMoreButton(
                        track = track,
                        selectedTrack = selectedTrack,
                        onSelect = { onMoreClick() },
                        onAddToPlaylist = onAddToPlaylist,
                        onOpenLyrics = onOpenLyrics,
                        onDeleteRequest = onDeleteRequest
                    )
                }
            }
        }
    }
}

@Composable
fun TrackLargeCardItem(
    track: TrackEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreClick: () -> Unit,
    selectedTrack: TrackEntity?,
    onAddToPlaylist: () -> Unit,
    onOpenLyrics: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (!track.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (track.mediaType == MediaType.VIDEO) Icons.Default.Videocam else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TrackMoreButton(
                    track = track,
                    selectedTrack = selectedTrack,
                    onSelect = { onMoreClick() },
                    onAddToPlaylist = onAddToPlaylist,
                    onOpenLyrics = onOpenLyrics,
                    onDeleteRequest = onDeleteRequest
                )
            }
        }
    }
}
