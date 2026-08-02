package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PlaylistEntity
import com.example.data.model.TrackEntity
import com.example.ui.MainViewModel

@Composable
fun PlaylistsScreen(
    viewModel: MainViewModel,
    playlists: List<PlaylistEntity>,
    modifier: Modifier = Modifier
) {
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    if (selectedPlaylistId != null) {
        val currentPlaylist = playlists.find { it.id == selectedPlaylistId }
        PlaylistDetailView(
            playlistName = currentPlaylist?.name ?: "Playlist",
            tracks = selectedPlaylistTracks,
            onBack = { viewModel.selectedPlaylistId.value = null },
            onPlayAll = {
                if (selectedPlaylistTracks.isNotEmpty()) {
                    viewModel.playTrack(selectedPlaylistTracks.first(), selectedPlaylistTracks)
                }
            },
            onShuffleAll = {
                if (selectedPlaylistTracks.isNotEmpty()) {
                    val shuffled = selectedPlaylistTracks.shuffled()
                    viewModel.playTrack(shuffled.first(), shuffled)
                    viewModel.playerEngine.toggleShuffle()
                }
            },
            onPlayTrack = { track -> viewModel.playTrack(track, selectedPlaylistTracks) },
            onRemoveTrack = { trackId -> viewModel.removeTrackFromPlaylist(selectedPlaylistId!!, trackId) },
            onToggleFavorite = { track -> viewModel.toggleFavorite(track) }
        )
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(bottom = 90.dp)
                        .testTag("create_playlist_fab")
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Create")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Playlist", fontWeight = FontWeight.Bold)
                    }
                }
            },
            modifier = modifier.fillMaxSize().testTag("playlists_screen")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
                )

                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No playlists created yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showCreateDialog = true }) {
                                Text("Create Your First Playlist")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(playlists) { playlist ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectedPlaylistId.value = playlist.id }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistPlay,
                                        contentDescription = "Playlist",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Custom Playlist",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }

                                    IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "New Playlist",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        placeholder = { Text("e.g. Synthwave Chill") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (playlistName.isNotBlank()) {
                                viewModel.createPlaylist(playlistName)
                                showCreateDialog = false
                            }
                        }) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlistName: String,
    tracks: List<TrackEntity>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onPlayTrack: (TrackEntity) -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = playlistName,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onPlayAll,
                enabled = tracks.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play All")
            }

            Button(
                onClick = onShuffleAll,
                enabled = tracks.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Shuffle, contentDescription = "Shuffle")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shuffle")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tracks in this playlist.\nAdd tracks from the Library tab!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tracks) { track ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TrackListItem(
                            track = track,
                            onClick = { onPlayTrack(track) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { onRemoveTrack(track.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
