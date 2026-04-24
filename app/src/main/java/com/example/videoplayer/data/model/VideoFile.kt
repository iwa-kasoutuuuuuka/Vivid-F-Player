package com.example.videoplayer.data.model

import android.net.Uri

data class VideoFile(
    val name: String,
    val uri: Uri,
    val size: Long,
    val lastModified: Long
)
