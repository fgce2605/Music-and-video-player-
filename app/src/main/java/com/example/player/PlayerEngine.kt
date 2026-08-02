package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.Equalizer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import com.example.data.model.MediaType
import com.example.data.model.RepeatMode
import com.example.data.model.SubtitleCue
import com.example.data.model.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VideoScaleMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    STRETCH("Stretch")
}

data class PlayerState(
    val currentTrack: TrackEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffle: Boolean = false,
    val queue: List<TrackEntity> = emptyList(),
    val queueIndex: Int = -1,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemainingSeconds: Int = 0,
    val activeSubtitleCue: String? = null,
    val activeLyricLine: String? = null,
    val eqPresetName: String = "Flat",
    val eqBandLevels: List<Short> = listOf(0, 0, 0, 0, 0),
    val isVideoFullscreen: Boolean = false,
    val isVideoLocked: Boolean = false,
    val videoBrightness: Float = 0.5f,
    val videoVolume: Float = 1.0f,
    val videoScaleMode: VideoScaleMode = VideoScaleMode.FIT,
    val videoAspectRatio: Float = 16f / 9f
)

class PlayerEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var mediaSession: MediaSession? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var subtitleCues: List<SubtitleCue> = emptyList()

    private var onTrackFinishedCallback: ((TrackEntity, Long) -> Unit)? = null

    init {
        com.example.service.MediaPlaybackService.activePlayerEngine = this
        setupMediaSession()
    }

    fun getMediaSessionToken(): android.media.session.MediaSession.Token? {
        return mediaSession?.sessionToken
    }

    fun cycleVideoScaleMode() {
        _playerState.update { s ->
            val nextMode = when (s.videoScaleMode) {
                VideoScaleMode.FIT -> VideoScaleMode.FILL
                VideoScaleMode.FILL -> VideoScaleMode.STRETCH
                VideoScaleMode.STRETCH -> VideoScaleMode.FIT
            }
            s.copy(videoScaleMode = nextMode)
        }
    }

    fun setOnTrackFinishedCallback(callback: (TrackEntity, Long) -> Unit) {
        onTrackFinishedCallback = callback
    }

    private fun setupMediaSession() {
        try {
            mediaSession = MediaSession(context, "OmniPlaySession").apply {
                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() { play() }
                    override fun onPause() { pause() }
                    override fun onSkipToNext() { playNext() }
                    override fun onSkipToPrevious() { playPrevious() }
                    override fun onSeekTo(pos: Long) { seekTo(pos) }
                })
                isActive = true
            }
        } catch (e: Exception) {
            Log.e("PlayerEngine", "MediaSession setup error: ${e.message}")
        }
    }

    fun playTrack(track: TrackEntity, newQueue: List<TrackEntity> = emptyList(), index: Int = -1) {
        val currentQueue = if (newQueue.isNotEmpty()) newQueue else _playerState.value.queue
        val targetIndex = if (index >= 0) index else currentQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _playerState.update {
            it.copy(
                currentTrack = track,
                queue = if (newQueue.isNotEmpty()) newQueue else it.queue,
                queueIndex = targetIndex,
                durationMs = track.durationMs
            )
        }

        subtitleCues = SrtSubtitleParser.parse(track.subtitleUrl)
        initAndPlay(track)
    }

    private fun initAndPlay(track: TrackEntity) {
        try {
            releaseMediaPlayer()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                val fileUri = Uri.parse(track.fileUrl)
                if (fileUri.scheme == "content" || fileUri.scheme == "file" || fileUri.scheme == "http" || fileUri.scheme == "https") {
                    setDataSource(context, fileUri)
                } else {
                    setDataSource(track.fileUrl)
                }

                setOnVideoSizeChangedListener { _, width, height ->
                    if (width > 0 && height > 0) {
                        val ratio = width.toFloat() / height.toFloat()
                        _playerState.update { s -> s.copy(videoAspectRatio = ratio) }
                    }
                }

                setOnPreparedListener { mp ->
                    val actualDuration = mp.duration.toLong()
                    _playerState.update { state ->
                        state.copy(
                            durationMs = if (actualDuration > 0) actualDuration else track.durationMs,
                            isPlaying = true
                        )
                    }
                    applySpeed(_playerState.value.playbackSpeed)
                    applyVolume(_playerState.value.volume, _playerState.value.isMuted)
                    setupEqualizer(mp.audioSessionId)
                    mp.start()
                    startProgressTracker()
                    updateMediaSessionState(PlaybackState.STATE_PLAYING)
                }

                setOnCompletionListener {
                    handleTrackCompletion()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("PlayerEngine", "MediaPlayer error: what=$what, extra=$extra for url=${track.fileUrl}")
                    _playerState.update { it.copy(isPlaying = false) }
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("PlayerEngine", "Error initializing player for ${track.fileUrl}: ${e.message}", e)
            _playerState.update { it.copy(isPlaying = false) }
        }
    }

    fun setDisplaySurface(holder: SurfaceHolder?) {
        try {
            mediaPlayer?.setDisplay(holder)
        } catch (e: Exception) {
            Log.e("PlayerEngine", "Error setting display surface: ${e.message}")
        }
    }

    fun play() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _playerState.update { s -> s.copy(isPlaying = true) }
                startProgressTracker()
                updateMediaSessionState(PlaybackState.STATE_PLAYING)
            }
        } ?: run {
            _playerState.value.currentTrack?.let { playTrack(it) }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playerState.update { s -> s.copy(isPlaying = false) }
                stopProgressTracker()
                updateMediaSessionState(PlaybackState.STATE_PAUSED)
            }
        }
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            val clamped = positionMs.coerceIn(0L, _playerState.value.durationMs)
            it.seekTo(clamped.toInt())
            _playerState.update { s -> s.copy(currentPositionMs = clamped) }
            updateSubtitlesAndLyrics(clamped)
        }
    }

    fun skipForward(seconds: Int = 10) {
        val newPos = _playerState.value.currentPositionMs + (seconds * 1000)
        seekTo(newPos)
    }

    fun skipBackward(seconds: Int = 10) {
        val newPos = _playerState.value.currentPositionMs - (seconds * 1000)
        seekTo(newPos)
    }

    fun playNext() {
        val state = _playerState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        val nextIndex = when {
            state.isShuffle -> (queue.indices).random()
            state.queueIndex < queue.size - 1 -> state.queueIndex + 1
            state.repeatMode == RepeatMode.REPEAT_ALL -> 0
            else -> return
        }

        playTrack(queue[nextIndex], queue, nextIndex)
    }

    fun playPrevious() {
        val state = _playerState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        if (state.currentPositionMs > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = when {
            state.queueIndex > 0 -> state.queueIndex - 1
            state.repeatMode == RepeatMode.REPEAT_ALL -> queue.size - 1
            else -> 0
        }

        playTrack(queue[prevIndex], queue, prevIndex)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playerState.update { it.copy(playbackSpeed = speed) }
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val params = mp.playbackParams ?: PlaybackParams()
                        params.speed = speed
                        mp.playbackParams = params
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerEngine", "Error setting playback speed: ${e.message}")
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        _playerState.update { it.copy(repeatMode = mode) }
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _playerState.update { it.copy(volume = clamped, isMuted = false, videoVolume = clamped) }
        applyVolume(clamped, false)
    }

    fun toggleMute() {
        val newMute = !_playerState.value.isMuted
        _playerState.update { it.copy(isMuted = newMute) }
        applyVolume(_playerState.value.volume, newMute)
    }

    private fun applyVolume(vol: Float, muted: Boolean) {
        val target = if (muted) 0f else vol
        mediaPlayer?.setVolume(target, target)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.update { it.copy(sleepTimerMinutes = 0, sleepTimerRemainingSeconds = 0) }
            return
        }

        val totalSeconds = minutes * 60
        _playerState.update { it.copy(sleepTimerMinutes = minutes, sleepTimerRemainingSeconds = totalSeconds) }

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _playerState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
            }
            pause()
            _playerState.update { it.copy(sleepTimerMinutes = 0, sleepTimerRemainingSeconds = 0) }
        }
    }

    fun setEqBandLevel(bandIndex: Short, levelDb: Short) {
        try {
            equalizer?.let { eq ->
                eq.setBandLevel(bandIndex, levelDb)
                val bandCount = eq.numberOfBands.toInt()
                val levels = (0 until bandCount).map { i -> eq.getBandLevel(i.toShort()) }
                _playerState.update { it.copy(eqBandLevels = levels, eqPresetName = "Custom") }
            }
        } catch (e: Exception) {
            Log.e("PlayerEngine", "Equalizer band error: ${e.message}")
        }
    }

    fun setEqPreset(presetName: String) {
        try {
            equalizer?.let { eq ->
                val numPresets = eq.numberOfPresets.toInt()
                for (i in 0 until numPresets) {
                    if (eq.getPresetName(i.toShort()).equals(presetName, ignoreCase = true)) {
                        eq.usePreset(i.toShort())
                        val bandCount = eq.numberOfBands.toInt()
                        val levels = (0 until bandCount).map { idx -> eq.getBandLevel(idx.toShort()) }
                        _playerState.update { it.copy(eqPresetName = presetName, eqBandLevels = levels) }
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerEngine", "Equalizer preset error: ${e.message}")
        }
    }

    fun setVideoFullscreen(fullscreen: Boolean) {
        _playerState.update { it.copy(isVideoFullscreen = fullscreen) }
    }

    fun toggleVideoLock() {
        _playerState.update { it.copy(isVideoLocked = !it.isVideoLocked) }
    }

    fun setVideoBrightness(brightness: Float) {
        _playerState.update { it.copy(videoBrightness = brightness.coerceIn(0.1f, 1.0f)) }
    }

    private fun setupEqualizer(sessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
                val bandCount = numberOfBands.toInt()
                val levels = (0 until bandCount).map { i -> getBandLevel(i.toShort()) }
                _playerState.update { it.copy(eqBandLevels = levels) }
            }
        } catch (e: Exception) {
            Log.e("PlayerEngine", "Failed to setup Equalizer: ${e.message}")
        }
    }

    private fun handleTrackCompletion() {
        val state = _playerState.value
        state.currentTrack?.let { track ->
            onTrackFinishedCallback?.invoke(track, state.currentPositionMs / 1000)
        }

        when (state.repeatMode) {
            RepeatMode.REPEAT_ONE -> {
                seekTo(0)
                play()
            }
            RepeatMode.REPEAT_ALL -> playNext()
            RepeatMode.OFF -> {
                if (state.queueIndex < state.queue.size - 1) {
                    playNext()
                } else {
                    _playerState.update { it.copy(isPlaying = false, currentPositionMs = 0) }
                    stopProgressTracker()
                    updateMediaSessionState(PlaybackState.STATE_STOPPED)
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (_playerState.value.isPlaying) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val currentMs = mp.currentPosition.toLong()
                        _playerState.update { it.copy(currentPositionMs = currentMs) }
                        updateSubtitlesAndLyrics(currentMs)
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    private fun updateSubtitlesAndLyrics(currentMs: Long) {
        val activeCue = subtitleCues.find { currentMs in it.startTimeMs..it.endTimeMs }?.text

        val lyricsText = _playerState.value.currentTrack?.lyrics
        val activeLyric = if (!lyricsText.isNullOrBlank()) {
            val lines = lyricsText.lines()
            var matchedLine: String? = null
            for (line in lines) {
                if (line.startsWith("[") && line.contains("]")) {
                    val timestampStr = line.substring(1, line.indexOf("]"))
                    val timeMs = parseLyricTimestamp(timestampStr)
                    if (currentMs >= timeMs) {
                        matchedLine = line.substring(line.indexOf("]") + 1).trim()
                    }
                }
            }
            matchedLine ?: lyricsText
        } else null

        _playerState.update {
            it.copy(
                activeSubtitleCue = activeCue,
                activeLyricLine = activeLyric
            )
        }
    }

    private fun parseLyricTimestamp(ts: String): Long {
        return try {
            val parts = ts.split(":")
            if (parts.size == 2) {
                val mins = parts[0].toLong()
                val secs = parts[1].toDouble()
                (mins * 60000) + (secs * 1000).toLong()
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun updateMediaSessionState(sessionState: Int) {
        try {
            val stateBuilder = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                            PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_SKIP_TO_NEXT or
                            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackState.ACTION_SEEK_TO
                )
                .setState(sessionState, _playerState.value.currentPositionMs, _playerState.value.playbackSpeed)
            mediaSession?.setPlaybackState(stateBuilder.build())

            val track = _playerState.value.currentTrack
            if (track != null) {
                val isPlaying = sessionState == PlaybackState.STATE_PLAYING
                if (isPlaying || sessionState == PlaybackState.STATE_PAUSED) {
                    com.example.service.MediaPlaybackService.updateNotification(
                        context,
                        track.title,
                        track.artist,
                        isPlaying
                    )
                } else if (sessionState == PlaybackState.STATE_STOPPED) {
                    com.example.service.MediaPlaybackService.stop(context)
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerEngine", "MediaSession update state error: ${e.message}")
        }
    }

    private fun releaseMediaPlayer() {
        stopProgressTracker()
        try {
            equalizer?.release()
            equalizer = null
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("PlayerEngine", "Error releasing player: ${e.message}")
        }
    }

    fun release() {
        releaseMediaPlayer()
        mediaSession?.release()
        mediaSession = null
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}
