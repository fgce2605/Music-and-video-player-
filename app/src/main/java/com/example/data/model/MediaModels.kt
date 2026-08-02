package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MediaType {
    AUDIO,
    VIDEO
}

enum class RepeatMode {
    OFF,
    REPEAT_ONE,
    REPEAT_ALL
}

enum class SortOption {
    TITLE,
    ARTIST,
    ALBUM,
    DATE_ADDED,
    DURATION
}

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val mediaType: MediaType = MediaType.AUDIO,
    val fileUrl: String,
    val thumbnailUrl: String? = null,
    val subtitleUrl: String? = null,
    val lyrics: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("trackId")]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long,
    val position: Int = 0
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val lastPositionSeconds: Long = 0L
)

data class PlaylistWithTrackCount(
    val playlist: PlaylistEntity,
    val trackCount: Int,
    val coverThumbnail: String? = null
)

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)
