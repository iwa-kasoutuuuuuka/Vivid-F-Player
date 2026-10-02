package com.example.videoplayer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.util.NaturalOrderComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class LocalVideoRepository(private val context: Context) : VideoRepository {

    companion object {
        private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "ts", "flv", "m4v", "3gp")
        private val subtitleExtensions = setOf("srt", "ass", "vtt")

        // プロセス全体で共有する字幕キャッシュ: folderUri -> List<Pair<SubtitleName, SubtitleUri>>
        private val subtitleCache = ConcurrentHashMap<String, List<Pair<String, Uri>>>()

        fun isSubtitleForVideo(subtitleFileName: String, videoBaseName: String): Boolean {
            val subBase = subtitleFileName.substringBeforeLast('.')
            return subBase.equals(videoBaseName, ignoreCase = true) ||
                   subBase.startsWith("${videoBaseName}.", ignoreCase = true) ||
                   subBase.startsWith("${videoBaseName}_", ignoreCase = true)
        }
    }

    override suspend fun getVideoFiles(folderUri: Uri): List<VideoFile> = withContext(Dispatchers.IO) {
        val folderKey = folderUri.toString()
        val videoList = mutableListOf<VideoFile>()
        val subtitleList = mutableListOf<Pair<String, Uri>>()

        val queried = tryFastQuery(folderUri, videoList, subtitleList)
        if (queried) {
            subtitleCache[folderKey] = subtitleList
            return@withContext videoList.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
        }

        // フォールバック: 従来の DocumentFile 方式
        val root = DocumentFile.fromTreeUri(context, folderUri)
        if (root == null || !root.canRead()) return@withContext emptyList()

        val files = root.listFiles()
        for (f in files) {
            val name = f.name ?: continue
            val ext = name.substringAfterLast('.', "").lowercase()
            if (f.isFile) {
                if (videoExtensions.contains(ext)) {
                    videoList.add(
                        VideoFile(
                            name = name,
                            uri = f.uri,
                            size = f.length(),
                            lastModified = f.lastModified(),
                            isRemote = false
                        )
                    )
                } else if (subtitleExtensions.contains(ext)) {
                    subtitleList.add(Pair(name, f.uri))
                }
            }
        }
        subtitleCache[folderKey] = subtitleList
        videoList.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
    }

    private fun tryFastQuery(
        folderUri: Uri,
        outVideos: MutableList<VideoFile>,
        outSubtitles: MutableList<Pair<String, Uri>>
    ): Boolean {
        if (!DocumentsContract.isTreeUri(folderUri)) return false
        try {
            val docId = DocumentsContract.getTreeDocumentId(folderUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, docId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue

                    val name = (if (nameIdx >= 0) cursor.getString(nameIdx) else null) ?: continue
                    val docChildId = (if (idIdx >= 0) cursor.getString(idIdx) else null) ?: continue
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    val lastModified = if (modIdx >= 0) cursor.getLong(modIdx) else 0L

                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docChildId)
                    val ext = name.substringAfterLast('.', "").lowercase()

                    if (videoExtensions.contains(ext)) {
                        outVideos.add(
                            VideoFile(
                                name = name,
                                uri = fileUri,
                                size = size,
                                lastModified = lastModified,
                                isRemote = false
                            )
                        )
                    } else if (subtitleExtensions.contains(ext)) {
                        outSubtitles.add(Pair(name, fileUri))
                    }
                }
                return true
            }
        } catch (_: Exception) {
            // 例外時はフォールバック
        }
        return false
    }

    override suspend fun getSubtitleFiles(folderUri: Uri, videoFileName: String): List<Uri> = withContext(Dispatchers.IO) {
        val folderKey = folderUri.toString()
        val videoBaseName = videoFileName.substringBeforeLast('.')

        // 1. キャッシュから検索（0ミリ秒・プロセス共有）
        val cached = subtitleCache[folderKey]
        if (cached != null) {
            return@withContext cached
                .filter { isSubtitleForVideo(it.first, videoBaseName) }
                .map { it.second }
        }

        // 2. キャッシュがない場合、フォルダを走査してキャッシュ構築
        getVideoFiles(folderUri)
        val refreshed = subtitleCache[folderKey] ?: emptyList()
        refreshed
            .filter { isSubtitleForVideo(it.first, videoBaseName) }
            .map { it.second }
    }
}
