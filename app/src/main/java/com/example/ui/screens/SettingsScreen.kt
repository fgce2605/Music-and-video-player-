package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.player.PlayerState
import com.example.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    playerState: PlayerState,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    var showClearCacheDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
        )

        // Appearance Card
        SettingsSectionHeader("APPEARANCE")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsRowItem(
                    icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                    title = "Dark Theme",
                    subtitle = if (isDarkTheme) "Dark violet neon palette (Default)" else "Light clean palette",
                    trailing = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleTheme() }
                        )
                    },
                    onClick = { viewModel.toggleTheme() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback & Audio Card
        SettingsSectionHeader("AUDIO & PLAYBACK ENGINE")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsRowItem(
                    icon = Icons.Default.Equalizer,
                    title = "Audio Equalizer (5-Band)",
                    subtitle = "Preset: ${playerState.eqPresetName}",
                    onClick = onOpenEqualizer
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                SettingsRowItem(
                    icon = Icons.Default.Timer,
                    title = "Sleep Timer",
                    subtitle = if (playerState.sleepTimerRemainingSeconds > 0)
                        "Timer active: ${playerState.sleepTimerRemainingSeconds / 60} minutes remaining"
                    else "Turn off playback automatically",
                    onClick = onOpenSleepTimer
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                SettingsRowItem(
                    icon = Icons.Default.Speed,
                    title = "Current Playback Speed",
                    subtitle = "${playerState.playbackSpeed}x multiplier",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Storage & Cache
        SettingsSectionHeader("STORAGE & CACHE")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsRowItem(
                    icon = Icons.Default.Storage,
                    title = "Storage Usage",
                    subtitle = "Local offline media cache: ~4.2 MB",
                    onClick = {}
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                SettingsRowItem(
                    icon = Icons.Default.CleaningServices,
                    title = "Clear Media Cache",
                    subtitle = "Frees up temporary audio and video buffer space",
                    onClick = { showClearCacheDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About App
        SettingsSectionHeader("ABOUT OMNIPLAY")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsRowItem(
                icon = Icons.Default.Info,
                title = "OmniPlay Media Player v1.0",
                subtitle = "Unified Audio & Video Engine • Jetpack Compose & Room",
                onClick = {}
            )
        }
    }

    if (showClearCacheDialog) {
        Dialog(onDismissRequest = { showClearCacheDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Clear Cache?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This will clear temporary audio and video buffers. Your playlists and imported tracks will remain safe.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            showClearCacheDialog = false
                        }) {
                            Text("Clear", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        if (trailing != null) {
            trailing()
        }
    }
}
