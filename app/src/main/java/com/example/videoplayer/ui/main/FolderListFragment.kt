package com.example.videoplayer.ui.main

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoplayer.data.manager.ResumeManager
import android.content.Intent
import android.net.Uri
import com.example.videoplayer.ui.MainActivity
import kotlinx.coroutines.flow.collect

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
            onDelete = { viewModel.removeFolder(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.folders.collect { folders ->
                adapter.submitList(folders)
            }
        }
        
        setupQuickResume()

        binding.btnAddFolder.setOnClickListener {
            selectFolderLauncher.launch(null)
        }
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
