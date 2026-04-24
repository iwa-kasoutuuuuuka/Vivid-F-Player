package com.example.videoplayer.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.data.repository.FileRepository
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(application)
    private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    
    private val _stopPlaybackEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val stopPlaybackEvent = _stopPlaybackEvent.asSharedFlow()

    private val _videoFiles = MutableStateFlow<List<VideoFile>>(emptyList())
    val videoFiles: StateFlow<List<VideoFile>> = _videoFiles

    private val _folders = MutableStateFlow<List<Uri>>(emptyList())
    val folders: StateFlow<List<Uri>> = _folders

    private val _currentFolderUri = MutableStateFlow<Uri?>(null)
    val currentFolderUri: StateFlow<Uri?> = _currentFolderUri

    private val _isBackgroundPlayEnabled = MutableStateFlow(prefs.getBoolean("bg_play", false))
    val isBackgroundPlayEnabled: StateFlow<Boolean> = _isBackgroundPlayEnabled

    private val _playbackSpeed = MutableStateFlow(prefs.getFloat("playback_speed", 1.0f))
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _repeatMode = MutableStateFlow(prefs.getInt("repeat_mode", Player.REPEAT_MODE_OFF))
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _shuffleModeEnabled = MutableStateFlow(prefs.getBoolean("shuffle_mode", false))
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled

    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

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

    fun removeFolder(uri: Uri) {
        val currentSet = prefs.getStringSet("folder_uris", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.remove(uri.toString())
        prefs.edit().putStringSet("folder_uris", currentSet).apply()
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

    fun setBackgroundPlayEnabled(enabled: Boolean) {
        _isBackgroundPlayEnabled.value = enabled
        prefs.edit().putBoolean("bg_play", enabled).apply()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        prefs.edit().putFloat("playback_speed", speed).apply()
    }

    fun setRepeatMode(mode: Int) {
        _repeatMode.value = mode
        prefs.edit().putInt("repeat_mode", mode).apply()
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        _shuffleModeEnabled.value = enabled
        prefs.edit().putBoolean("shuffle_mode", enabled).apply()
    }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                kotlinx.coroutines.delay(minutes * 60 * 1000L)
                _sleepTimerMinutes.value = 0
                _stopPlaybackEvent.emit(Unit)
            }
        }
    }
}
