package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MediaType
import com.example.data.model.TrackEntity

@Composable
fun LyricsDialog(
    track: TrackEntity,
    onSaveLyrics: (String) -> Unit,
    onSaveSubtitles: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var lyricsText by remember { mutableStateOf(track.lyrics ?: "") }
    var subtitleText by remember { mutableStateOf(track.subtitleUrl ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("lyrics_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (track.mediaType == MediaType.VIDEO) "Edit Subtitles / Captions" else "Edit Track Lyrics",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (track.mediaType == MediaType.VIDEO) {
                    OutlinedTextField(
                        value = subtitleText,
                        onValueChange = { subtitleText = it },
                        label = { Text("SRT Subtitles format") },
                        placeholder = { Text("1\n00:00:01,000 --> 00:00:05,000\nSample Subtitle") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = lyricsText,
                        onValueChange = { lyricsText = it },
                        label = { Text("Plain or Synced Lyrics ([00:15] Lyrics line...)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    TextButton(
                        onClick = {
                            if (track.mediaType == MediaType.VIDEO) {
                                onSaveSubtitles(subtitleText)
                            } else {
                                onSaveLyrics(lyricsText)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
