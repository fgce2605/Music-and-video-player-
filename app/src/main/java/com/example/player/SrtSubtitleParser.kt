package com.example.player

import com.example.data.model.SubtitleCue

object SrtSubtitleParser {
    fun parse(subtitleText: String?): List<SubtitleCue> {
        if (subtitleText.isNullOrBlank()) return emptyList()
        val cues = mutableListOf<SubtitleCue>()
        val blocks = subtitleText!!.trim().split(Regex("\n\\s*\n"))

        for (block in blocks) {
            val lines = block.trim().lines()
            if (lines.size < 2) continue

            // Find the time line containing '-->'
            val timeLine = lines.find { it.contains("-->") } ?: continue
            val timeParts = timeLine.split("-->").map { it.trim() }
            if (timeParts.size < 2) continue

            val startTimeMs = parseTimestampToMs(timeParts[0])
            val endTimeMs = parseTimestampToMs(timeParts[1])

            // The remaining lines are subtitle text
            val textLines = lines.filter { !it.contains("-->") && !it.trim().all { char -> char.isDigit() } }
            val text = textLines.joinToString("\n").trim()

            if (text.isNotEmpty()) {
                cues.add(SubtitleCue(startTimeMs, endTimeMs, text))
            }
        }
        return cues.sortedBy { it.startTimeMs }
    }

    private fun parseTimestampToMs(timestamp: String): Long {
        return try {
            val clean = timestamp.replace(',', '.').trim()
            val parts = clean.split(":")
            if (parts.size == 3) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val secondsWithMs = parts[2].toDouble()
                val seconds = secondsWithMs.toLong()
                val ms = ((secondsWithMs - seconds) * 1000).toLong()
                (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + ms
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
