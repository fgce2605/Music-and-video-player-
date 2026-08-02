package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.player.PlayerEngine

class MediaPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "omniplay_playback_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_OR_UPDATE = "com.example.service.START_OR_UPDATE"
        const val ACTION_STOP = "com.example.service.STOP"
        const val ACTION_PLAY = "com.example.service.PLAY"
        const val ACTION_PAUSE = "com.example.service.PAUSE"
        const val ACTION_NEXT = "com.example.service.NEXT"
        const val ACTION_PREVIOUS = "com.example.service.PREVIOUS"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        var activePlayerEngine: PlayerEngine? = null

        fun updateNotification(context: Context, title: String, artist: String, isPlaying: Boolean) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Ignore if service cannot be started in background on Android 12+
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_OR_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "OmniPlay Media"
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: "Playing"
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                val notification = buildNotification(title, artist, isPlaying)
                startForeground(NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
            ACTION_PLAY -> {
                activePlayerEngine?.play()
            }
            ACTION_PAUSE -> {
                activePlayerEngine?.pause()
            }
            ACTION_NEXT -> {
                activePlayerEngine?.playNext()
            }
            ACTION_PREVIOUS -> {
                activePlayerEngine?.playPrevious()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows media controls for active audio/video playback"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, artist: String, isPlaying: Boolean): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseActionIntent = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MediaPlaybackService::class.java).apply { action = playPauseActionIntent },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = android.app.Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(contentIntent)
                .setOngoing(isPlaying)
                .setOnlyAlertOnce(true)
                .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                .addAction(
                    android.app.Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                        "Previous", prevIntent
                    ).build()
                )
                .addAction(
                    android.app.Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(this, playPauseIcon),
                        playPauseTitle, playPauseIntent
                    ).build()
                )
                .addAction(
                    android.app.Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_next),
                        "Next", nextIntent
                    ).build()
                )

            activePlayerEngine?.getMediaSessionToken()?.let { token ->
                val mediaStyle = android.app.Notification.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(token)
                builder.setStyle(mediaStyle)
            }

            return builder.build()
        } else {
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(contentIntent)
                .setOngoing(isPlaying)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
                .addAction(playPauseIcon, playPauseTitle, playPauseIntent)
                .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
                .build()
        }
    }
}
