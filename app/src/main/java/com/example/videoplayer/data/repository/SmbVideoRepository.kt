package com.example.videoplayer.data.repository

import android.net.Uri
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.util.NaturalOrderComparator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmbVideoRepository : VideoRepository {

    private val videoExtensions = setOf("mp4", "mkv", "avi")

    private val subtitleExtensions = setOf("srt", "ass", "vtt")

    override suspend fun getVideoFiles(uri: Uri): List<VideoFile> = withContext(Dispatchers.IO) {
        try {
            // SMB URI format: smb://user:password@host/share/path/
            val smbFile = SmbFile(uri.toString())
            if (!smbFile.isDirectory) return@withContext emptyList<VideoFile>()

            smbFile.listFiles()
                .filter { it.isFile && videoExtensions.contains(it.name.substringAfterLast('.').lowercase()) }
                .map { 
                    VideoFile(
                        name = it.name,
                        uri = Uri.parse(it.url.toString()),
                        size = it.length(),
                        lastModified = it.lastModified(),
                        isRemote = true
                    )
                }
                .sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getSubtitleFiles(folderUri: Uri, videoFileName: String): List<Uri> = withContext(Dispatchers.IO) {
        try {
            val smbDir = SmbFile(folderUri.toString())
            val videoBaseName = videoFileName.substringBeforeLast('.')

            smbDir.listFiles()
                .filter { 
                    it.isFile && 
                    it.name.startsWith(videoBaseName) && 
                    subtitleExtensions.contains(it.name.substringAfterLast('.').lowercase()) 
                }
                .map { Uri.parse(it.url.toString()) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
