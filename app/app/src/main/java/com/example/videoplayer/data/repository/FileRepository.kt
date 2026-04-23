package com.example.videoplayer.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.util.NaturalOrderComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileRepository(private val context: Context) {

    private val videoExtensions = setOf("mp4", "mkv", "avi")

    /**
     * 指定されたフォルダ内の動画ファイルを取得し、自然順でソートして返却します。
     */
    suspend fun getVideoFiles(folderUri: Uri): List<VideoFile> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, folderUri)
        if (root == null || !root.canRead()) return@withContext emptyList<VideoFile>()

        root.listFiles()
            .filter { it.isFile && videoExtensions.contains(it.name?.substringAfterLast('.')?.lowercase()) }
            .map { 
                VideoFile(
                    name = it.name ?: "Unknown",
                    uri = it.uri,
                    size = it.length(),
                    lastModified = it.lastModified()
                )
            }
            .sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
    }
}
