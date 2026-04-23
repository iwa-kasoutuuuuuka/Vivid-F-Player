package com.example.videoplayer.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.videoplayer.databinding.DialogSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collect

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

    var onSpeedChanged: ((Float) -> Unit)? = null
    var currentSpeed: Float = 1.0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 再生速度の初期値設定
        when (currentSpeed) {
            0.5f -> binding.rgSpeed.check(binding.rgSpeed.getChildAt(0).id)
            1.0f -> binding.rgSpeed.check(binding.rgSpeed.getChildAt(1).id)
            1.5f -> binding.rgSpeed.check(binding.rgSpeed.getChildAt(2).id)
            2.0f -> binding.rgSpeed.check(binding.rgSpeed.getChildAt(3).id)
        }

        binding.rgSpeed.setOnCheckedChangeListener { _, checkedId ->
            val speed = when (checkedId) {
                binding.rgSpeed.getChildAt(0).id -> 0.5f
                binding.rgSpeed.getChildAt(1).id -> 1.0f
                binding.rgSpeed.getChildAt(2).id -> 1.5f
                binding.rgSpeed.getChildAt(3).id -> 2.0f
                else -> 1.0f
            }
            onSpeedChanged?.invoke(speed)
        }

        val adapter = FolderListAdapter(
            onClick = { /* Settings内ではクリックで遷移しない */ },
            onDelete = { viewModel.removeFolder(it) }
        )
        binding.rvFolders.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvFolders.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.folders.collect { folders ->
                adapter.submitList(folders)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
