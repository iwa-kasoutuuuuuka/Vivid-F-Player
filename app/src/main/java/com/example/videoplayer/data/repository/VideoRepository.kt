package com.example.videoplayer.data.repository

import android.net.Uri
import com.example.videoplayer.data.model.VideoFile

interface VideoRepository {
    suspend fun getVideoFiles(uri: Uri): List<VideoFile>
}
