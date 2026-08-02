package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
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

    suspend fun scanMediaStore(context: Context): Int {
        var scannedCount = 0
        try {
            val existingUrls = trackDao.getAllFileUrls().toHashSet()
            val newTracks = mutableListOf<TrackEntity>()

            // 1. Scan Audio files from MediaStore
            val audioProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
            )
            val audioSelection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} > 1000"
            val audioCursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioProjection,
                audioSelection,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )

            audioCursor?.use { c ->
                val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val albumIdCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    if (idCol < 0) continue
                    val id = c.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

                    if (!existingUrls.contains(contentUri)) {
                        val rawTitle = if (titleCol >= 0) c.getString(titleCol) else null
                        val title = if (!rawTitle.isNullOrBlank()) rawTitle else "Audio Track $id"

                        val rawArtist = if (artistCol >= 0) c.getString(artistCol) else null
                        val artist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") rawArtist else "Unknown Artist"

                        val rawAlbum = if (albumCol >= 0) c.getString(albumCol) else null
                        val album = if (!rawAlbum.isNullOrBlank() && rawAlbum != "<unknown>") rawAlbum else "Local Library"

                        val duration = if (durCol >= 0) c.getLong(durCol) else 0L

                        val albumId = if (albumIdCol >= 0) c.getLong(albumIdCol) else -1L
                        val artworkUri = if (albumId >= 0) {
                            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                        } else null

                        newTracks.add(
                            TrackEntity(
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                mediaType = MediaType.AUDIO,
                                fileUrl = contentUri,
                                thumbnailUrl = artworkUri
                            )
                        )
                        existingUrls.add(contentUri)
                    }
                }
            }

            // 2. Scan Video files from MediaStore
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.ARTIST,
                MediaStore.Video.Media.ALBUM,
                MediaStore.Video.Media.DURATION
            )
            val videoCursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            videoCursor?.use { c ->
                val idCol = c.getColumnIndex(MediaStore.Video.Media._ID)
                val titleCol = c.getColumnIndex(MediaStore.Video.Media.TITLE)
                val artistCol = c.getColumnIndex(MediaStore.Video.Media.ARTIST)
                val albumCol = c.getColumnIndex(MediaStore.Video.Media.ALBUM)
                val durCol = c.getColumnIndex(MediaStore.Video.Media.DURATION)

                while (c.moveToNext()) {
                    if (idCol < 0) continue
                    val id = c.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

                    if (!existingUrls.contains(contentUri)) {
                        val rawTitle = if (titleCol >= 0) c.getString(titleCol) else null
                        val title = if (!rawTitle.isNullOrBlank()) rawTitle else "Video File $id"

                        val rawArtist = if (artistCol >= 0) c.getString(artistCol) else null
                        val artist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") rawArtist else "Local Video"

                        val rawAlbum = if (albumCol >= 0) c.getString(albumCol) else null
                        val album = if (!rawAlbum.isNullOrBlank() && rawAlbum != "<unknown>") rawAlbum else "Gallery Videos"

                        val duration = if (durCol >= 0) c.getLong(durCol) else 0L

                        newTracks.add(
                            TrackEntity(
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                mediaType = MediaType.VIDEO,
                                fileUrl = contentUri,
                                thumbnailUrl = contentUri
                            )
                        )
                        existingUrls.add(contentUri)
                    }
                }
            }

            if (newTracks.isNotEmpty()) {
                trackDao.insertTracks(newTracks)
                scannedCount = newTracks.size
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error scanning MediaStore: ${e.message}")
        }
        return scannedCount
    }
}
