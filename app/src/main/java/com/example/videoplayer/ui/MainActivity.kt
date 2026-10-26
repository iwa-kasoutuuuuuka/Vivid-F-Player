package com.example.videoplayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.videoplayer.R
import com.example.videoplayer.databinding.ActivityMainBinding
import com.example.videoplayer.ui.main.FileListFragment
import com.example.videoplayer.ui.main.FolderListFragment
import com.example.videoplayer.ui.main.FolderSelectionFragment
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
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.folders.collect { folders ->
                    if (folders.isEmpty()) {
                        showFolderSelection()
                    } else if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is FileListFragment) {
                        showFolderList()
                    }
                }
            }
        }
    }

    fun showFolderSelection() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FolderSelectionFragment())
            .commitAllowingStateLoss()
    }

    fun showFolderList() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FolderListFragment())
            .commitAllowingStateLoss()
    }

    fun navigateToFileList(uri: Uri) {
        // FileListFragment側でsetFolder()するため、ここでは読み込まない（SMBで二重列挙になる）
        // FileListFragment loads the folder itself; loading here too listed SMB shares twice
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FileListFragment.newInstance(uri))
            .addToBackStack(null)
            .commit()
    }

    fun onFolderSelected(uri: Uri) {
        if (uri.scheme == "content") {
            // 最小権限の原則: 読み取り権限のみ永続化（読み取り専用SDカード/OTGでのSecurityExceptionも防止）
            // Principle of least privilege: only persist READ permission (also prevents SecurityException on read-only media)
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        viewModel.addFolder(uri)
    }
}
