package com.example.videoplayer.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.videoplayer.R
import com.example.videoplayer.databinding.ActivityMainBinding
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.videoplayer.ui.main.FileListFragment
import com.example.videoplayer.ui.main.FolderListFragment
import com.example.videoplayer.ui.main.FolderSelectionFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: com.example.videoplayer.ui.main.MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            viewModel.folders.collect { folders ->
                if (folders.isEmpty()) {
                    showFolderSelection()
                } else if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is FileListFragment) {
                    showFolderList()
                }
            }
        }
    }

    fun showFolderSelection() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FolderSelectionFragment())
            .commit()
    }

    fun showFolderList() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FolderListFragment())
            .commit()
    }

    fun navigateToFileList(uri: Uri) {
        viewModel.setFolder(uri)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FileListFragment.newInstance(uri))
            .addToBackStack(null)
            .commit()
    }

    fun onFolderSelected(uri: Uri) {
        contentResolver.takePersistableUriPermission(uri, 
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or 
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        viewModel.addFolder(uri)
    }
}
