package com.example.videoplayer.data.repository

import android.net.Uri
import com.example.videoplayer.data.manager.SmbCredentialStore
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.util.NaturalOrderComparator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class SmbVideoRepository(private val credentialStore: SmbCredentialStore) : VideoRepository {

    private val videoExtensions = setOf("mp4", "mkv", "avi")
    private val subtitleExtensions = setOf("srt", "ass", "vtt")

    // キャッシュ: folderUri -> List<Pair<SubtitleName, SubtitleUri>>
    private val subtitleCache = ConcurrentHashMap<String, List<Pair<String, Uri>>>()

    private fun open(uri: Uri): SmbFile {
        // SMB URI format: smb://host[:port]/share/path/ (認証情報は暗号化ストアから / credentials come from the store)
        val url = uri.toString()
        return SmbFile(url, credentialStore.contextFor(url))
    }

    override suspend fun getVideoFiles(folderUri: Uri): List<VideoFile> = withContext(Dispatchers.IO) {
        val folderKey = folderUri.toString()
        try {
            val smbFile = open(folderUri)
            if (!smbFile.isDirectory) return@withContext emptyList<VideoFile>()

            val allFiles = smbFile.listFiles()
            val videoList = mutableListOf<VideoFile>()
            val subtitleList = mutableListOf<Pair<String, Uri>>()

            for (it in allFiles) {
                if (!it.isFile) continue
                val name = it.name
                val ext = name.substringAfterLast('.', "").lowercase()
                if (videoExtensions.contains(ext)) {
                    videoList.add(
                        VideoFile(
                            name = name,
                            uri = Uri.parse(it.url.toString()),
                            size = it.length(),
                            lastModified = it.lastModified(),
                            isRemote = true
                        )
                    )
                } else if (subtitleExtensions.contains(ext)) {
                    subtitleList.add(Pair(name, Uri.parse(it.url.toString())))
                }
            }

            subtitleCache[folderKey] = subtitleList
            videoList.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getSubtitleFiles(folderUri: Uri, videoFileName: String): List<Uri> = withContext(Dispatchers.IO) {
        val folderKey = folderUri.toString()
        val videoBaseName = videoFileName.substringBeforeLast('.')

        // 1. キャッシュから即座に返却（ネットワーク往復ゼロ・0ms）
        val cached = subtitleCache[folderKey]
        if (cached != null) {
            return@withContext cached
                .filter { it.first.startsWith(videoBaseName) }
                .map { it.second }
        }

        // 2. キャッシュがない場合、フォルダをスキャンしてキャッシュ構築
        getVideoFiles(folderUri)
        val refreshed = subtitleCache[folderKey] ?: emptyList()
        refreshed
            .filter { it.first.startsWith(videoBaseName) }
            .map { it.second }
    }
}
