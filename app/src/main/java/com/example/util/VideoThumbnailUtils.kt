package com.example.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import com.example.data.model.TrackEntity
import java.io.File
import java.io.FileOutputStream

object VideoThumbnailUtils {

    fun getOrCreateVideoThumbnail(context: Context, track: TrackEntity): String? {
        val cacheDir = File(context.cacheDir, "video_thumbnails").apply { mkdirs() }
        val filename = "vthumb_${track.id}_${track.fileUrl.hashCode()}.jpg"
        val cacheFile = File(cacheDir, filename)

        if (cacheFile.exists() && cacheFile.length() > 0) {
            return "file://${cacheFile.absolutePath}"
        }

        var bitmap: Bitmap? = null

        // 1. Try ContentResolver.loadThumbnail (Android Q+) or MediaStore.Video.Thumbnails for content:// URIs
        if (track.fileUrl.startsWith("content://")) {
            try {
                val uri = Uri.parse(track.fileUrl)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bitmap = context.contentResolver.loadThumbnail(uri, Size(320, 240), null)
                } else {
                    val videoId = ContentUris.parseId(uri)
                    @Suppress("DEPRECATION")
                    bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        videoId,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                }
            } catch (e: Exception) {
                Log.d("VideoThumbnailUtils", "ContentResolver loadThumbnail failed: ${e.message}")
            }
        }

        // 2. Fallback to MediaMetadataRetriever
        if (bitmap == null) {
            val retriever = MediaMetadataRetriever()
            try {
                val uri = Uri.parse(track.fileUrl)
                if (track.fileUrl.startsWith("content://") || track.fileUrl.startsWith("file://")) {
                    retriever.setDataSource(context, uri)
                } else if (track.fileUrl.startsWith("http://") || track.fileUrl.startsWith("https://")) {
                    retriever.setDataSource(track.fileUrl, HashMap())
                } else {
                    retriever.setDataSource(track.fileUrl)
                }
                bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            } catch (e: Exception) {
                Log.e("VideoThumbnailUtils", "MediaMetadataRetriever failed for ${track.fileUrl}: ${e.message}")
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        // 3. Fallback to ThumbnailUtils.createVideoThumbnail for file paths
        if (bitmap == null && !track.fileUrl.startsWith("content://") && !track.fileUrl.startsWith("http")) {
            try {
                val file = File(track.fileUrl)
                if (file.exists()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        bitmap = ThumbnailUtils.createVideoThumbnail(file, Size(320, 240), null)
                    } else {
                        @Suppress("DEPRECATION")
                        bitmap = ThumbnailUtils.createVideoThumbnail(
                            file.absolutePath,
                            MediaStore.Images.Thumbnails.MINI_KIND
                        )
                    }
                }
            } catch (e: Exception) {
                Log.d("VideoThumbnailUtils", "ThumbnailUtils failed: ${e.message}")
            }
        }

        // Save bitmap to cache if generated
        if (bitmap != null) {
            try {
                FileOutputStream(cacheFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                return "file://${cacheFile.absolutePath}"
            } catch (e: Exception) {
                Log.e("VideoThumbnailUtils", "Error saving thumbnail to file: ${e.message}")
            }
        }

        return null
    }
}
