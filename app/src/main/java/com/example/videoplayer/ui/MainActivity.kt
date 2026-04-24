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
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: com.example.videoplayer.ui.main.MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 通知権限の要求 (Android 13以上) / Request notification permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    android.widget.Toast.makeText(this, getString(R.string.no_notification_permission), android.widget.Toast.LENGTH_LONG).show()
                }
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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
