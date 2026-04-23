package com.example.videoplayer.data.manager

import android.content.Context

/**
 * フォルダごとの再生レジューム情報を管理します。
 */
class ResumeManager(context: Context) {
    private val prefs = context.getSharedPreferences("resume_prefs", Context.MODE_PRIVATE)

    fun saveResumePosition(folderUri: String, fileName: String, positionMs: Long) {
        prefs.edit()
            .putString("last_file_$folderUri", fileName)
            .putLong("last_pos_$folderUri", positionMs)
            .apply()
    }

    fun getLastFileName(folderUri: String): String? = prefs.getString("last_file_$folderUri", null)
    fun getLastPosition(folderUri: String): Long = prefs.getLong("last_pos_$folderUri", 0L)
}
