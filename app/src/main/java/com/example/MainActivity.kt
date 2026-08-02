package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaType
import com.example.data.model.TrackEntity
import com.example.ui.MainViewModel
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.LyricsDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.VideoPlayerView
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.OmniPlayTheme

enum class BottomTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIBRARY("Library", Icons.Default.LibraryMusic),
    SEARCH("Search", Icons.Default.Search),
    PLAYLISTS("Playlists", Icons.Default.QueueMusic),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            OmniPlayTheme(darkTheme = isDarkTheme) {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var editingTrackLyrics by remember { mutableStateOf<TrackEntity?>(null) }

    val playerState by viewModel.playerState.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()
    val filteredTracks by viewModel.filteredLibraryTracks.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val favorites by viewModel.favoriteTracks.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Persistent Mini-Player Bar
                if (playerState.currentTrack != null) {
                    MiniPlayerBar(
                        playerState = playerState,
                        onTogglePlayPause = { viewModel.playerEngine.togglePlayPause() },
                        onSkipNext = { viewModel.playerEngine.playNext() },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                        onClickMiniPlayer = { showFullPlayer = true }
                    )
                }

                // Bottom Tab Navigation Bar
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    BottomTab.values().forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    allTracks = allTracks,
                    recentlyPlayed = recentlyPlayed,
                    mostPlayed = mostPlayed,
                    favorites = favorites,
                    onNavigateToSearch = { selectedTab = 2 }
                )
                1 -> LibraryScreen(
                    viewModel = viewModel,
                    tracks = filteredTracks,
                    playlists = playlists,
                    onOpenLyricsDialog = { track -> editingTrackLyrics = track }
                )
                2 -> SearchScreen(viewModel = viewModel)
                3 -> PlaylistsScreen(
                    viewModel = viewModel,
                    playlists = playlists
                )
                4 -> SettingsScreen(
                    viewModel = viewModel,
                    playerState = playerState,
                    onOpenEqualizer = { showEqualizer = true },
                    onOpenSleepTimer = { showSleepTimer = true }
                )
            }
        }
    }

    // Fullscreen Player View / Sheet
    if (showFullPlayer && playerState.currentTrack != null) {
        val currentTrack = playerState.currentTrack!!
        if (currentTrack.mediaType == MediaType.VIDEO) {
            VideoPlayerView(
                playerState = playerState,
                playerEngine = viewModel.playerEngine,
                onBack = { showFullPlayer = false }
            )
        } else {
            NowPlayingSheet(
                playerState = playerState,
                playerEngine = viewModel.playerEngine,
                onDismiss = { showFullPlayer = false },
                onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                onOpenEqualizer = { showEqualizer = true },
                onOpenLyricsDialog = { track -> editingTrackLyrics = track },
                onOpenSleepTimer = { showSleepTimer = true }
            )
        }
    }

    // Dialogs
    if (showEqualizer) {
        EqualizerDialog(
            playerState = playerState,
            playerEngine = viewModel.playerEngine,
            onDismiss = { showEqualizer = false }
        )
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            playerState = playerState,
            playerEngine = viewModel.playerEngine,
            onDismiss = { showSleepTimer = false }
        )
    }

    if (editingTrackLyrics != null) {
        LyricsDialog(
            track = editingTrackLyrics!!,
            onSaveLyrics = { lyrics -> viewModel.updateLyrics(editingTrackLyrics!!.id, lyrics) },
            onSaveSubtitles = { subtitles -> viewModel.updateSubtitle(editingTrackLyrics!!.id, subtitles) },
            onDismiss = { editingTrackLyrics = null }
        )
    }
}
