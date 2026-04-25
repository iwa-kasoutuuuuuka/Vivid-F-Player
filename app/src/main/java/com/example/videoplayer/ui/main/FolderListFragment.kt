package com.example.videoplayer.ui.main

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoplayer.data.manager.ResumeManager
import android.content.Intent
import com.example.videoplayer.R
import com.example.videoplayer.ui.MainActivity
import com.example.videoplayer.databinding.FragmentFolderListBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FolderListFragment : Fragment() {
    private var _binding: FragmentFolderListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private val selectFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { (activity as? MainActivity)?.onFolderSelected(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFolderListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val adapter = FolderListAdapter(
            onClick = { (activity as? MainActivity)?.navigateToFileList(it) },
            onDelete = { uri ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.delete_folder_confirm))
                    .setPositiveButton("OK") { _, _ -> viewModel.removeFolder(uri) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // フォルダ一覧の監視 / Monitor folder list
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.folders.collect { folders ->
                    adapter.submitList(folders)
                }
            }
        }
        
        setupQuickResume()

        binding.btnAddFolder.setOnClickListener {
            selectFolderLauncher.launch(null)
        }

        binding.btnAddSmbFolder.setOnClickListener {
            showAddSmbDialog()
        }
    }

    private fun showAddSmbDialog() {
        val dialogBinding = com.example.videoplayer.databinding.DialogAddSmbBinding.inflate(layoutInflater)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_VideoPlayer_Dialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnConnect.setOnClickListener {
            val server = dialogBinding.etServer.text.toString().trim()
            val share = dialogBinding.etShare.text.toString().trim()
            val user = dialogBinding.etUsername.text.toString().trim()
            val pass = dialogBinding.etPassword.text.toString().trim()

            if (server.isNotEmpty() && share.isNotEmpty()) {
                // smb://[user:password@]host/share/
                val userInfo = if (user.isNotEmpty()) {
                    if (pass.isNotEmpty()) "$user:$pass@" else "$user@"
                } else ""
                
                val smbUrl = "smb://$userInfo$server/$share/"
                viewModel.addSmbFolder(smbUrl)
                dialog.dismiss()
            } else {
                android.widget.Toast.makeText(requireContext(), R.string.invalid_smb_path, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun setupQuickResume() {
        val resumeManager = ResumeManager(requireContext())
        val lastPlayed = resumeManager.getGlobalLastPlayed()
        
        if (lastPlayed != null) {
            val fUri = lastPlayed.first
            val fName = lastPlayed.second
            val vUri = lastPlayed.third
            val pos = lastPlayed.fourth
            
            binding.cardLastPlayed.visibility = View.VISIBLE
            binding.tvLastFileName.text = fName
            
            binding.cardLastPlayed.setOnClickListener {
                val intent = Intent(requireContext(), com.example.videoplayer.ui.player.PlayerActivity::class.java).apply {
                    putExtra("video_uri", Uri.parse(vUri))
                    putExtra("folder_uri", fUri)
                    putExtra("resume_pos", pos)
                }
                startActivity(intent)
            }
        } else {
            binding.cardLastPlayed.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
