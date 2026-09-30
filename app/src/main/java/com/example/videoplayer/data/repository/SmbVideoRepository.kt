package com.example.videoplayer.data.repository

import android.net.Uri
import com.example.videoplayer.data.manager.SmbCredentialStore
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.util.NaturalOrderComparator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmbVideoRepository(private val credentialStore: SmbCredentialStore) : VideoRepository {

    private val videoExtensions = setOf("mp4", "mkv", "avi")

    private val subtitleExtensions = setOf("srt", "ass", "vtt")

    private fun open(uri: Uri): SmbFile {
        // SMB URI format: smb://host[:port]/share/path/ (認証情報は暗号化ストアから / credentials come from the store)
        val url = uri.toString()
        return SmbFile(url, credentialStore.contextFor(url))
    }

    override suspend fun getVideoFiles(folderUri: Uri): List<VideoFile> = withContext(Dispatchers.IO) {
        try {
            val smbFile = open(folderUri)
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
            val smbDir = open(folderUri)
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
