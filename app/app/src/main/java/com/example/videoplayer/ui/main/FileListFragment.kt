package com.example.videoplayer.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoplayer.data.manager.ResumeManager
import com.example.videoplayer.databinding.FragmentFileListBinding
import com.example.videoplayer.ui.player.PlayerActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FileListFragment : Fragment() {
    private var _binding: FragmentFileListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: FileListAdapter

    companion object {
        fun newInstance(uri: Uri) = FileListFragment().apply {
            arguments = Bundle().apply {
                putParcelable("folder_uri", uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFileListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val folderUri = arguments?.getParcelable<Uri>("folder_uri") ?: return
        viewModel.setFolder(folderUri)

        adapter = FileListAdapter { videoFile ->
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("video_uri", videoFile.uri)
                putExtra("folder_uri", folderUri.toString())
                val resumeManager = ResumeManager(requireContext())
                if (resumeManager.getLastFileName(folderUri.toString()) == videoFile.name) {
                    putExtra("resume_pos", resumeManager.getLastPosition(folderUri.toString()))
                }
            }
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.tvFolderName.text = folderUri.lastPathSegment
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnSettings.setOnClickListener {
            SettingsBottomSheet().show(parentFragmentManager, "settings")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.videoFiles.collectLatest { files ->
                adapter.submitList(files)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
