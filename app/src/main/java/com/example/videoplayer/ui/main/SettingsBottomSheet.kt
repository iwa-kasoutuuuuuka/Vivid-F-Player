package com.example.videoplayer.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.videoplayer.databinding.DialogSettingsBinding
import com.example.videoplayer.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SettingsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 再生速度の監視 / Monitor playback speed
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.playbackSpeed.collect { speed ->
                    when (speed) {
                        0.5f -> binding.rgSpeed.check(binding.rgSpeed.getChildAt(0).id)
                        1.0f -> binding.rgSpeed.check(binding.rgSpeed.getChildAt(1).id)
                        1.5f -> binding.rgSpeed.check(binding.rgSpeed.getChildAt(2).id)
                        2.0f -> binding.rgSpeed.check(binding.rgSpeed.getChildAt(3).id)
                    }
                }
            }
        }

        binding.rgSpeed.setOnCheckedChangeListener { _, checkedId ->
            val speed = when (checkedId) {
                binding.rgSpeed.getChildAt(0).id -> 0.5f
                binding.rgSpeed.getChildAt(1).id -> 1.0f
                binding.rgSpeed.getChildAt(2).id -> 1.5f
                binding.rgSpeed.getChildAt(3).id -> 2.0f
                else -> 1.0f
            }
            viewModel.setPlaybackSpeed(speed)
        }

        // バックグラウンド再生設定の監視 / Monitor background play setting
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.isBackgroundPlayEnabled.collect { enabled ->
                    binding.swBackgroundPlay.isChecked = enabled
                }
            }
        }

        binding.swBackgroundPlay.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBackgroundPlayEnabled(isChecked)
        }

        val adapter = FolderListAdapter(
            onClick = { /* No-op in settings */ },
            onDelete = { viewModel.removeFolder(it) }
        )
        binding.rvFolders.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvFolders.adapter = adapter

        // フォルダ一覧の監視 / Monitor folder list
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.folders.collect { folders ->
                    adapter.submitList(folders)
                }
            }
        }

        // トラブルシューティング表示 / Show troubleshooting dialog
        binding.btnTroubleshooting.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.bg_troubleshooting_title)
                .setMessage(R.string.bg_troubleshooting_content)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
