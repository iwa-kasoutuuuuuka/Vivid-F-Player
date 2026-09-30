package com.example.videoplayer.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videoplayer.data.manager.AppSettings
import com.example.videoplayer.data.manager.SmbCredentialStore
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.data.repository.CompositeVideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CompositeVideoRepository(application)
    private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    private val credentialStore = SmbCredentialStore.getInstance(application)

    // 再生設定はアプリ全体で共有 / Playback settings are shared app-wide
    private val settings = AppSettings.getInstance(application)
    val stopPlaybackEvent = settings.stopPlaybackEvent
    val isBackgroundPlayEnabled = settings.isBackgroundPlayEnabled
    val isAutoPipEnabled = settings.isAutoPipEnabled
    val playbackSpeed = settings.playbackSpeed
    val repeatMode = settings.repeatMode
    val shuffleModeEnabled = settings.shuffleModeEnabled
    val sleepTimerMinutes = settings.sleepTimerMinutes

    private val _videoFiles = MutableStateFlow<List<VideoFile>>(emptyList())
    val videoFiles: StateFlow<List<VideoFile>> = _videoFiles

    private val _folders = MutableStateFlow<List<Uri>>(emptyList())
    val folders: StateFlow<List<Uri>> = _folders

    private val _currentFolderUri = MutableStateFlow<Uri?>(null)
    val currentFolderUri: StateFlow<Uri?> = _currentFolderUri

    init {
        loadFolders()
    }

    private fun loadFolders() {
        val uris = prefs.getStringSet("folder_uris", emptySet()) ?: emptySet()
        _folders.value = uris.map { Uri.parse(it) }
    }

    fun addFolder(uri: Uri) {
        val currentSet = prefs.getStringSet("folder_uris", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(uri.toString())
        prefs.edit().putStringSet("folder_uris", currentSet).apply()
        loadFolders()
    }

    /**
     * SMBフォルダを登録。認証情報はURLに含めず、暗号化ストアに保存します。
     * Registers an SMB folder; credentials are kept out of the URL and saved to the encrypted store.
     */
    fun addSmbFolder(url: String, username: String, password: String) {
        credentialStore.save(url, username, password)
        val currentSet = prefs.getStringSet("folder_uris", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(url)
        prefs.edit().putStringSet("folder_uris", currentSet).apply()
        loadFolders()
    }

    fun removeFolder(uri: Uri) {
        val currentSet = prefs.getStringSet("folder_uris", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.remove(uri.toString())
        prefs.edit().putStringSet("folder_uris", currentSet).apply()
        if (uri.scheme == "smb") credentialStore.remove(uri.toString(), currentSet)
        loadFolders()
    }

    fun setFolder(uri: Uri) {
        _currentFolderUri.value = uri
        loadFiles(uri)
    }

    private fun loadFiles(uri: Uri) {
        viewModelScope.launch {
            _videoFiles.value = repository.getVideoFiles(uri)
        }
    }

    fun setBackgroundPlayEnabled(enabled: Boolean) = settings.setBackgroundPlayEnabled(enabled)
    fun setAutoPipEnabled(enabled: Boolean) = settings.setAutoPipEnabled(enabled)
    fun setPlaybackSpeed(speed: Float) = settings.setPlaybackSpeed(speed)
    fun setRepeatMode(mode: Int) = settings.setRepeatMode(mode)
    fun setShuffleModeEnabled(enabled: Boolean) = settings.setShuffleModeEnabled(enabled)
    fun setSleepTimer(minutes: Int) = settings.setSleepTimer(minutes)
}
