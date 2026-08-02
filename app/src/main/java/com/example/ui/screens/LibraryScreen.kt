package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaType
import com.example.data.model.PlaylistEntity
import com.example.data.model.SortOption
import com.example.data.model.TrackEntity
import com.example.ui.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    tracks: List<TrackEntity>,
    playlists: List<PlaylistEntity>,
    onOpenLyricsDialog: (TrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTrackForMenu by remember { mutableStateOf<TrackEntity?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importLocalFile(it, context) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 90.dp)
                    .testTag("import_media_fab")
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = "Import")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Media", fontWeight = FontWeight.Bold)
                }
            }
        },
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
                                text = { Text("Sort by ${option.name.lowercase().capitalize()}") },
                                onClick = {
                                    viewModel.librarySortOption.value = option
                                    showSortMenu = false
                                }
                            )
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
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracks) { track ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TrackListItem(
                                track = track,
                                onClick = { viewModel.playTrack(track, tracks) },
                                onToggleFavorite = { viewModel.toggleFavorite(track) },
                                modifier = Modifier.weight(1f)
                            )

                            Box {
                                IconButton(onClick = { selectedTrackForMenu = track }) {
                                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                                }

                                DropdownMenu(
                                    expanded = selectedTrackForMenu == track,
                                    onDismissRequest = { selectedTrackForMenu = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Add to Playlist") },
                                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                                        onClick = {
                                            showAddToPlaylistDialog = true
                                            selectedTrackForMenu = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (track.mediaType == MediaType.VIDEO) "Edit Subtitles" else "Edit Lyrics") },
                                        leadingIcon = { Icon(Icons.Default.Subtitles, contentDescription = null) },
                                        onClick = {
                                            onOpenLyricsDialog(track)
                                            selectedTrackForMenu = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Track", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            viewModel.playerEngine.pause()
                                            selectedTrackForMenu = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddToPlaylistDialog && selectedTrackForMenu != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddToPlaylistDialog = false }) {
            androidx.compose.material3.Card(
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
                                        viewModel.addTrackToPlaylist(playlist.id, selectedTrackForMenu!!.id)
                                        showAddToPlaylistDialog = false
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

private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
