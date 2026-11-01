package com.example.videoplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailLoader {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // 利用可能メモリの1/8をサムネイルキャッシュに割り当て

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun getCached(key: String): Bitmap? = memoryCache.get(key)

    suspend fun loadThumbnail(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val key = uri.toString()
        memoryCache.get(key)?.let { return@withContext it }

        // SMB動画はネットワーク遅延・通信量抑制のためスキップ
        if (uri.scheme == "smb") return@withContext null

        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    context.contentResolver.loadThumbnail(uri, Size(160, 100), null)
                } catch (_: Exception) {
                    loadWithRetriever(context, uri)
                }
            } else {
                loadWithRetriever(context, uri)
            }

            if (bitmap != null) {
                memoryCache.put(key, bitmap)
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun loadWithRetriever(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val pfd = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (_: Exception) {
                null
            }

            if (pfd != null) {
                pfd.use {
                    retriever.setDataSource(it.fileDescriptor)
                    extractFrame(retriever)
                }
            } else {
                retriever.setDataSource(context, uri)
                extractFrame(retriever)
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun extractFrame(retriever: MediaMetadataRetriever): Bitmap? {
        val targetWidth = 160
        val targetHeight = 100
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetWidth, targetHeight)
                ?: retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetWidth, targetHeight)
        } else {
            val fullBitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (fullBitmap != null) {
                val scaled = Bitmap.createScaledBitmap(fullBitmap, targetWidth, targetHeight, true)
                if (scaled != fullBitmap) fullBitmap.recycle()
                scaled
            } else {
                null
            }
        }
    }
}
