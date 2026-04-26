package com.example.videoplayer.data.repository

import android.content.Context
import android.net.Uri
import com.example.videoplayer.data.model.VideoFile

class CompositeVideoRepository(private val context: Context) : VideoRepository {
    private val localRepository = LocalVideoRepository(context)
    private val smbRepository = SmbVideoRepository()

    override suspend fun getVideoFiles(uri: Uri): List<VideoFile> {
        return when (uri.scheme) {
            "smb" -> smbRepository.getVideoFiles(uri)
            else -> localRepository.getVideoFiles(uri)
        }
    }

    override suspend fun getSubtitleFiles(folderUri: Uri, videoFileName: String): List<Uri> {
        return if (folderUri.scheme == "smb") {
            smbRepository.getSubtitleFiles(folderUri, videoFileName)
        } else {
            localRepository.getSubtitleFiles(folderUri, videoFileName)
        }
    }
}
