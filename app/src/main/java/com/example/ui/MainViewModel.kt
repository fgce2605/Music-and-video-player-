package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.MediaType
import com.example.data.model.RepeatMode
import com.example.data.model.SortOption
import com.example.data.model.TrackEntity
import com.example.data.repository.MediaRepository
import com.example.player.PlayerEngine
import com.example.player.PlayerState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = MediaRepository(db.trackDao(), db.playlistDao(), db.playHistoryDao())
    val playerEngine = PlayerEngine(application)

    val playerState: StateFlow<PlayerState> = playerEngine.playerState

    // UI state flows
    val allTracks: StateFlow<List<TrackEntity>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<TrackEntity>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<TrackEntity>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<TrackEntity>> = repository.mostPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query & filtering
    val searchQuery = MutableStateFlow("")
    val searchResults: StateFlow<List<TrackEntity>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allTracks
            else repository.searchTracks(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Library filtering and sorting
    val selectedMediaTypeTab = MutableStateFlow<MediaType?>(null) // null = ALL
    val librarySortOption = MutableStateFlow(SortOption.TITLE)

    val filteredLibraryTracks: StateFlow<List<TrackEntity>> = combine(
        allTracks,
        selectedMediaTypeTab,
        librarySortOption
    ) { tracks, tab, sort ->
        val filtered = if (tab == null) tracks else tracks.filter { it.mediaType == tab }
        when (sort) {
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM -> filtered.sortedBy { it.album.lowercase() }
            SortOption.DATE_ADDED -> filtered.sortedByDescending { it.createdAt }
            SortOption.DURATION -> filtered.sortedByDescending { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Playlist Tracks
    val selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistTracks: StateFlow<List<TrackEntity>> = selectedPlaylistId
        .flatMapLatest { id ->
            if (id != null) repository.getTracksForPlaylist(id)
            else MutableStateFlow(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Preferences & Theme
    val isDarkTheme = MutableStateFlow(true)

    // Toast & Event channel
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        playerEngine.setOnTrackFinishedCallback { track, posSec ->
            viewModelScope.launch {
                repository.addPlayHistory(track.id, posSec)
            }
        }
    }

    fun playTrack(track: TrackEntity, playlist: List<TrackEntity> = emptyList()) {
        val queue = if (playlist.isNotEmpty()) playlist else allTracks.value
        playerEngine.playTrack(track, newQueue = queue)
        viewModelScope.launch {
            repository.addPlayHistory(track.id, 0L)
        }
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            val newFav = !track.isFavorite
            repository.updateFavorite(track.id, newFav)
            _toastEvent.emit(if (newFav) "Added to Favorites" else "Removed from Favorites")
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createPlaylist(name.trim())
                _toastEvent.emit("Playlist '$name' created")
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
            if (selectedPlaylistId.value == id) {
                selectedPlaylistId.value = null
            }
            _toastEvent.emit("Playlist deleted")
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            _toastEvent.emit("Added to playlist")
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            _toastEvent.emit("Removed from playlist")
        }
    }

    fun updateLyrics(trackId: Long, newLyrics: String) {
        viewModelScope.launch {
            repository.updateLyrics(trackId, newLyrics)
            _toastEvent.emit("Lyrics updated")
        }
    }

    fun updateSubtitle(trackId: Long, subtitleUrl: String) {
        viewModelScope.launch {
            repository.updateSubtitle(trackId, subtitleUrl)
            _toastEvent.emit("Subtitles updated")
        }
    }

    fun importLocalFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val fileName = getFileName(context, uri) ?: "Local Track"
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val isVideo = mimeType.startsWith("video/") || fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".webm")

                val title = fileName.substringBeforeLast(".")
                val newTrack = TrackEntity(
                    title = title,
                    artist = "Local Library",
                    album = if (isVideo) "Local Video" else "Local Audio",
                    durationMs = 180000L, // Estimated / fallback
                    mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
                    fileUrl = uri.toString(),
                    thumbnailUrl = if (isVideo) "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600&auto=format&fit=crop" else null
                )

                val id = repository.insertTrack(newTrack)
                _toastEvent.emit("Imported '$title'")
            } catch (e: Exception) {
                _toastEvent.emit("Failed to import file: ${e.message}")
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.release()
    }
}
