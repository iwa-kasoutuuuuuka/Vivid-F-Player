package com.example.videoplayer.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.data.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(application)
    private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    
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
}
