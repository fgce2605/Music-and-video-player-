package com.example.ui.components

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.TrackEntity
import com.example.player.PlayerEngine
import com.example.player.PlayerState
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerView(
    playerState: PlayerState,
    playerEngine: PlayerEngine,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    var showOverlay by remember { mutableStateOf(true) }
    var gestureStatusText by remember { mutableStateOf<String?>(null) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Auto-hide controls after 3s
    LaunchedEffect(showOverlay, playerState.isPlaying) {
        if (showOverlay && playerState.isPlaying && !playerState.isVideoLocked) {
            delay(3500)
            showOverlay = false
        }
    }

    // Clear gesture status text after 1s
    LaunchedEffect(gestureStatusText) {
        if (gestureStatusText != null) {
            delay(1200)
            gestureStatusText = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(playerState.isVideoLocked) {
                detectTapGestures(
                    onTap = {
                        showOverlay = !showOverlay
                    },
                    onDoubleTap = { offset ->
                        if (!playerState.isVideoLocked) {
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                playerEngine.skipBackward(10)
                                gestureStatusText = "« 10s"
                            } else {
                                playerEngine.skipForward(10)
                                gestureStatusText = "10s »"
                            }
                        }
                    }
                )
            }
            .pointerInput(playerState.isVideoLocked) {
                if (!playerState.isVideoLocked) {
                    var dragYAccumulator = 0f
                    var dragXAccumulator = 0f

                    detectDragGestures(
                        onDragStart = {
                            dragYAccumulator = 0f
                            dragXAccumulator = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragYAccumulator += dragAmount.y
                            dragXAccumulator += dragAmount.x

                            val screenWidth = size.width
                            val touchX = change.position.x

                            if (kotlin.math.abs(dragYAccumulator) > kotlin.math.abs(dragXAccumulator)) {
                                val delta = -dragYAccumulator / 800f
                                if (touchX < screenWidth / 2) {
                                    // Left side: Brightness
                                    val newBright = (playerState.videoBrightness + delta).coerceIn(0.1f, 1.0f)
                                    playerEngine.setVideoBrightness(newBright)
                                    gestureStatusText = "Brightness: ${(newBright * 100).toInt()}%"
                                } else {
                                    // Right side: Volume
                                    val newVol = (playerState.volume + delta).coerceIn(0f, 1f)
                                    playerEngine.setVolume(newVol)
                                    gestureStatusText = "Volume: ${(newVol * 100).toInt()}%"
                                }
                            }
                        }
                    )
                }
            }
            .testTag("video_player_container")
    ) {
        // Video Surface
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            playerEngine.setDisplaySurface(holder)
                        }

                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            playerEngine.setDisplaySurface(null)
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtitle Overlay
        if (!playerState.activeSubtitleCue.isNull_or_blank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showOverlay) 80.dp else 32.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = playerState.activeSubtitleCue!!,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Gesture feedback overlay pill
        if (gestureStatusText != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text(
                    text = gestureStatusText!!,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }

        // Control Overlay
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )

                    // Lock Button
                    IconButton(onClick = { playerEngine.toggleVideoLock() }) {
                        Icon(
                            imageVector = if (playerState.isVideoLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = if (playerState.isVideoLocked) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }

                // Center Play/Pause Controls (if not locked)
                if (!playerState.isVideoLocked) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        IconButton(
                            onClick = { playerEngine.skipBackward(10) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Skip -10s",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { playerEngine.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        IconButton(
                            onClick = { playerEngine.skipForward(10) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Skip +10s",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // Bottom Control Bar (if not locked)
                if (!playerState.isVideoLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Slider(
                            value = if (playerState.durationMs > 0) playerState.currentPositionMs.toFloat() else 0f,
                            onValueChange = { pos -> playerEngine.seekTo(pos.toLong()) },
                            valueRange = 0f..(if (playerState.durationMs > 0) playerState.durationMs.toFloat() else 1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${formatDuration(playerState.currentPositionMs)} / ${formatDuration(playerState.durationMs)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    TextButton(onClick = { showSpeedMenu = !showSpeedMenu }) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "Speed",
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${playerState.playbackSpeed}x", color = Color.White)
                                    }

                                    DropdownMenu(
                                        expanded = showSpeedMenu,
                                        onDismissRequest = { showSpeedMenu = false }
                                    ) {
                                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                            DropdownMenuItem(
                                                text = { Text("${speed}x") },
                                                onClick = {
                                                    playerEngine.setPlaybackSpeed(speed)
                                                    showSpeedMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = {
                                    playerEngine.setVideoFullscreen(!playerState.isVideoFullscreen)
                                }) {
                                    Icon(
                                        imageVector = if (playerState.isVideoFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = Color.White
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

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
