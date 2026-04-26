package com.example.videoplayer.data.manager

import android.content.Context
import com.example.videoplayer.util.Quadruple

/**
 * フォルダごとの再生レジューム情報を管理します。
 */
class ResumeManager(context: Context) {
    private val prefs = context.getSharedPreferences("resume_prefs", Context.MODE_PRIVATE)

    fun saveResumePosition(folderUri: String, fileName: String, videoUri: String, positionMs: Long, durationMs: Long) {
        prefs.edit()
            .putString("last_file_$folderUri", fileName)
            .putLong("last_pos_$folderUri", positionMs)
            .putLong("pos_$videoUri", positionMs)
            .putLong("dur_$videoUri", durationMs)
            // グローバルな最終再生情報を更新
            .putString("global_last_folder", folderUri)
            .putString("global_last_file", fileName)
            .putString("global_last_uri", videoUri)
            .putLong("global_last_pos", positionMs)
            .apply()
    }

    fun getFileResumePosition(videoUri: String): Long = prefs.getLong("pos_$videoUri", 0L)
    fun getFileDuration(videoUri: String): Long = prefs.getLong("dur_$videoUri", 0L)
    fun getLastFileName(folderUri: String): String? = prefs.getString("last_file_$folderUri", null)
    fun getLastPosition(folderUri: String): Long = prefs.getLong("last_pos_$folderUri", 0L)

    fun getGlobalLastPlayed(): Quadruple<String, String, String, Long>? {
        val folder = prefs.getString("global_last_folder", null) ?: return null
        val file = prefs.getString("global_last_file", null) ?: return null
        val uri = prefs.getString("global_last_uri", null) ?: return null
        val pos = prefs.getLong("global_last_pos", 0L)
        return Quadruple(folder, file, uri, pos)
    }
}


