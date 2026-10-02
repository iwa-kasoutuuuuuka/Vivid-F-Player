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
                        0.5f -> binding.rgSpeed.check(R.id.rb_speed_05)
                        1.0f -> binding.rgSpeed.check(R.id.rb_speed_10)
                        1.5f -> binding.rgSpeed.check(R.id.rb_speed_15)
                        2.0f -> binding.rgSpeed.check(R.id.rb_speed_20)
                    }
                }
            }
        }

        binding.rgSpeed.setOnCheckedChangeListener { _, checkedId ->
            val speed = when (checkedId) {
                R.id.rb_speed_05 -> 0.5f
                R.id.rb_speed_10 -> 1.0f
                R.id.rb_speed_15 -> 1.5f
                R.id.rb_speed_20 -> 2.0f
                else -> 1.0f
            }
            viewModel.setPlaybackSpeed(speed)
        }

        // スキップ秒数の監視 / Monitor skip duration
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.skipSeconds.collect { sec ->
                    when (sec) {
                        5 -> binding.rgSkipDuration.check(R.id.rb_skip_5)
                        10 -> binding.rgSkipDuration.check(R.id.rb_skip_10)
                        30 -> binding.rgSkipDuration.check(R.id.rb_skip_30)
                        60 -> binding.rgSkipDuration.check(R.id.rb_skip_60)
                    }
                }
            }
        }

        binding.rgSkipDuration.setOnCheckedChangeListener { _, checkedId ->
            val sec = when (checkedId) {
                R.id.rb_skip_5 -> 5
                R.id.rb_skip_10 -> 10
                R.id.rb_skip_30 -> 30
                R.id.rb_skip_60 -> 60
                else -> 10
            }
            viewModel.setSkipSeconds(sec)
        }

        // 長押し倍速の監視 / Monitor long press speed
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.longPressSpeed.collect { speed ->
                    when (speed) {
                        1.5f -> binding.rgLongPressSpeed.check(R.id.rb_long_15)
                        2.0f -> binding.rgLongPressSpeed.check(R.id.rb_long_20)
                        2.5f -> binding.rgLongPressSpeed.check(R.id.rb_long_25)
                        3.0f -> binding.rgLongPressSpeed.check(R.id.rb_long_30)
                    }
                }
            }
        }

        binding.rgLongPressSpeed.setOnCheckedChangeListener { _, checkedId ->
            val speed = when (checkedId) {
                R.id.rb_long_15 -> 1.5f
                R.id.rb_long_20 -> 2.0f
                R.id.rb_long_25 -> 2.5f
                R.id.rb_long_30 -> 3.0f
                else -> 2.0f
            }
            viewModel.setLongPressSpeed(speed)
        }

        // ボイスブーストの監視 / Monitor voice boost
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.isVoiceBoostEnabled.collect { enabled ->
                    binding.swVoiceBoost.isChecked = enabled
                }
            }
        }

        binding.swVoiceBoost.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setVoiceBoostEnabled(isChecked)
        }

        // 極低輝度ナイトモードの監視 / Monitor night mode
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.isNightModeEnabled.collect { enabled ->
                    binding.swNightMode.isChecked = enabled
                }
            }
        }

        binding.swNightMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setNightModeEnabled(isChecked)
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

        // ホームでPiP表示の監視 / Monitor auto Picture-in-Picture setting
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.isAutoPipEnabled.collect { enabled ->
                    binding.swAutoPip.isChecked = enabled
                }
            }
        }

        binding.swAutoPip.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoPipEnabled(isChecked)
        }

        // リピートモードの監視 / Monitor repeat mode
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.repeatMode.collect { mode ->
                    when (mode) {
                        androidx.media3.common.Player.REPEAT_MODE_OFF -> binding.rgRepeatMode.check(binding.rgRepeatMode.getChildAt(0).id)
                        androidx.media3.common.Player.REPEAT_MODE_ONE -> binding.rgRepeatMode.check(binding.rgRepeatMode.getChildAt(1).id)
                        androidx.media3.common.Player.REPEAT_MODE_ALL -> binding.rgRepeatMode.check(binding.rgRepeatMode.getChildAt(2).id)
                    }
                }
            }
        }

        binding.rgRepeatMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                binding.rgRepeatMode.getChildAt(0).id -> androidx.media3.common.Player.REPEAT_MODE_OFF
                binding.rgRepeatMode.getChildAt(1).id -> androidx.media3.common.Player.REPEAT_MODE_ONE
                binding.rgRepeatMode.getChildAt(2).id -> androidx.media3.common.Player.REPEAT_MODE_ALL
                else -> androidx.media3.common.Player.REPEAT_MODE_OFF
            }
            viewModel.setRepeatMode(mode)
        }

        // シャッフル設定の監視 / Monitor shuffle setting
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.shuffleModeEnabled.collect { enabled ->
                    binding.swShuffle.isChecked = enabled
                }
            }
        }

        binding.swShuffle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShuffleModeEnabled(isChecked)
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

        // おやすみタイマーの監視 / Monitor sleep timer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.sleepTimerMinutes.collect { minutes ->
                    val text = if (minutes > 0) "${minutes}分後に停止 / Stop in ${minutes}m" else "OFF"
                    binding.tvSleepTimerValue.text = text
                }
            }
        }

        binding.btnSleepTimer.setOnClickListener {
            val options = arrayOf("OFF", "15分 / 15 min", "30分 / 30 min", "60分 / 60 min")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("おやすみタイマー / Sleep Timer")
                .setItems(options) { _, which ->
                    val minutes = when (which) {
                        1 -> 15
                        2 -> 30
                        3 -> 60
                        else -> 0
                    }
                    viewModel.setSleepTimer(minutes)
                }
                .show()
        }

        // バッテリー最適化設定の追加 / Add battery optimization setting
        binding.btnBatteryOptimization.setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val intent = android.content.Intent()
        val packageName = requireContext().packageName
        val pm = requireContext().getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            // Already ignored, open app details for Xiaomi users to check "No restrictions"
            intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = android.net.Uri.fromParts("package", packageName, null)
        } else {
            intent.action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = android.net.Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
