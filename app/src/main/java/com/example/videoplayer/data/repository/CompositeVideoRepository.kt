package com.example.videoplayer.data.repository

import android.content.Context
import android.net.Uri
import com.example.videoplayer.data.model.VideoFile

class CompositeVideoRepository(private val context: Context) : VideoRepository {
    private val localRepository = LocalVideoRepository(context)
    private val smbRepository = SmbVideoRepository(
        com.example.videoplayer.data.manager.SmbCredentialStore.getInstance(context)
    )

    override suspend fun getVideoFiles(folderUri: Uri): List<VideoFile> {
        return when (folderUri.scheme) {
            "smb" -> smbRepository.getVideoFiles(folderUri)
            else -> localRepository.getVideoFiles(folderUri)
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
