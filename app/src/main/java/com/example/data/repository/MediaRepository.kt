package com.example.data.repository

import com.example.data.local.PlayHistoryDao
import com.example.data.local.PlaylistDao
import com.example.data.local.TrackDao
import com.example.data.model.MediaType
import com.example.data.model.PlayHistoryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.TrackEntity
import kotlinx.coroutines.flow.Flow

class MediaRepository(
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val playHistoryDao: PlayHistoryDao
) {
    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()
    val favoriteTracks: Flow<List<TrackEntity>> = trackDao.getFavoriteTracks()
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    val recentlyPlayed: Flow<List<TrackEntity>> = playHistoryDao.getRecentlyPlayed()
    val mostPlayed: Flow<List<TrackEntity>> = playHistoryDao.getMostPlayed()

    fun getTracksByType(type: MediaType): Flow<List<TrackEntity>> = trackDao.getTracksByType(type)

    fun searchTracks(query: String): Flow<List<TrackEntity>> = trackDao.searchTracks(query)

    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>> =
        playlistDao.getTracksForPlaylist(playlistId)

    suspend fun insertTrack(track: TrackEntity): Long = trackDao.insertTrack(track)

    suspend fun updateFavorite(trackId: Long, isFavorite: Boolean) {
        trackDao.updateFavorite(trackId, isFavorite)
    }

    suspend fun updateLyrics(trackId: Long, lyrics: String) {
        val track = trackDao.getTrackById(trackId)
        if (track != null) {
            trackDao.updateTrack(track.copy(lyrics = lyrics))
        }
    }

    suspend fun updateSubtitle(trackId: Long, subtitleUrl: String) {
        val track = trackDao.getTrackById(trackId)
        if (track != null) {
            trackDao.updateTrack(track.copy(subtitleUrl = subtitleUrl))
        }
    }

    suspend fun deleteTrack(track: TrackEntity) = trackDao.deleteTrack(track)

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long, position: Int = 0) {
        playlistDao.insertCrossRef(PlaylistTrackCrossRef(playlistId, trackId, position))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun addPlayHistory(trackId: Long, lastPositionSeconds: Long) {
        playHistoryDao.insertHistory(
            PlayHistoryEntity(
                trackId = trackId,
                lastPositionSeconds = lastPositionSeconds
            )
        )
    }

    suspend fun getLastPosition(trackId: Long): Long {
        return playHistoryDao.getLastPosition(trackId) ?: 0L
    }

    suspend fun seedInitialDataIfEmpty() {
        if (trackDao.getTrackCount() == 0) {
            val sampleTracks = listOf(
                TrackEntity(
                    title = "Midnight Synthwave Drive",
                    artist = "Neon Horizon",
                    album = "Retro Glow",
                    durationMs = 210000L,
                    mediaType = MediaType.AUDIO,
                    fileUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=600&auto=format&fit=crop",
                    lyrics = """[00:10] Cruising through the neon rain
[00:25] City lights flash by again
[00:45] Feel the bass line in your spine
[01:10] Lost in space and out of time
[01:40] Midnight drive, set me free
[02:20] Endless synthwave symphony""",
                    isFavorite = true
                ),
                TrackEntity(
                    title = "Acoustic Sunset Chill",
                    artist = "Luna Resonance",
                    album = "Golden Hour Sessions",
                    durationMs = 185000L,
                    mediaType = MediaType.AUDIO,
                    fileUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=600&auto=format&fit=crop",
                    lyrics = """[00:15] Soft golden rays upon the shore
[00:35] Gentle ocean whispers once more
[01:05] Acoustic strings drift in the breeze
[01:30] Peace found underneath the trees""",
                    isFavorite = true
                ),
                TrackEntity(
                    title = "Lo-Fi Coffee Study Beats",
                    artist = "Aura Beats",
                    album = "Chillhop Dreams",
                    durationMs = 240000L,
                    mediaType = MediaType.AUDIO,
                    fileUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    thumbnailUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?q=80&w=600&auto=format&fit=crop",
                    lyrics = "Instrumental Track - Pure Lo-Fi Beats & Ambient Vinyl Crackle",
                    isFavorite = false
                ),
                TrackEntity(
                    title = "Big Buck Bunny Movie Teaser",
                    artist = "Blender Foundation",
                    album = "Open Movie Project",
                    durationMs = 596000L,
                    mediaType = MediaType.VIDEO,
                    fileUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    thumbnailUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600&auto=format&fit=crop",
                    subtitleUrl = """1
00:00:01,000 --> 00:00:04,000
Welcome to Big Buck Bunny in 4K Ultra HD!

2
00:00:05,000 --> 00:00:09,000
Enjoy pristine local and stream video playback with custom gesture controls.

3
00:00:10,000 --> 00:00:15,000
Swipe left/right to seek, swipe vertical for volume and brightness!""",
                    lyrics = "Official Trailer and Sample Movie Clip for Video Playback Demo",
                    isFavorite = true
                ),
                TrackEntity(
                    title = "Elephants Dream Sci-Fi Short",
                    artist = "Orange Open Movie",
                    album = "Cinematic Shorts",
                    durationMs = 653000L,
                    mediaType = MediaType.VIDEO,
                    fileUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    thumbnailUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=600&auto=format&fit=crop",
                    subtitleUrl = """1
00:00:02,000 --> 00:00:06,000
At the center of the giant machine...

2
00:00:07,000 --> 00:00:12,000
Probing the boundaries of digital animation and immersive surround sound.""",
                    isFavorite = false
                ),
                TrackEntity(
                    title = "Cyberpunk Ambient Flow",
                    artist = "Pixel Echoes",
                    album = "Future Systems",
                    durationMs = 290000L,
                    mediaType = MediaType.AUDIO,
                    fileUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=600&auto=format&fit=crop",
                    lyrics = "Electronic Soundscape - Cyberpunk Atmosphere",
                    isFavorite = false
                )
            )

            trackDao.insertTracks(sampleTracks)

            // Seed a starter playlist
            val playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = "Chill Vibes Favorites"))
            val insertedTracks = trackDao.getTrackCount()
            if (insertedTracks > 0) {
                playlistDao.insertCrossRef(PlaylistTrackCrossRef(playlistId, 1L, 0))
                playlistDao.insertCrossRef(PlaylistTrackCrossRef(playlistId, 2L, 1))
            }
        }
    }
}
