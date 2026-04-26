package com.example.videoplayer.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.util.NaturalOrderComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalVideoRepository(private val context: Context) : VideoRepository {

    private val videoExtensions = setOf("mp4", "mkv", "avi")
    private val subtitleExtensions = setOf("srt", "ass", "vtt")
    
    override suspend fun getVideoFiles(folderUri: Uri): List<VideoFile> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, folderUri)
        if (root == null || !root.canRead()) return@withContext emptyList<VideoFile>()

        root.listFiles()
            .filter { it.isFile && videoExtensions.contains(it.name?.substringAfterLast('.')?.lowercase()) }
            .map { 
                VideoFile(
                    name = it.name ?: "Unknown",
                    uri = it.uri,
                    size = it.length(),
                    lastModified = it.lastModified(),
                    isRemote = false
                )
            }
            .sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
    }

    override suspend fun getSubtitleFiles(folderUri: Uri, videoFileName: String): List<Uri> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        val videoBaseName = videoFileName.substringBeforeLast('.')

        root.listFiles()
            .filter { 
                it.isFile && 
                it.name?.startsWith(videoBaseName) == true && 
                subtitleExtensions.contains(it.name?.substringAfterLast('.')?.lowercase()) 
            }
            .map { it.uri }
    }
}
