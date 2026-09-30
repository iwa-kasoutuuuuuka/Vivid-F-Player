package com.example.videoplayer.ui.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.videoplayer.R
import com.example.videoplayer.data.manager.ResumeManager
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.data.repository.CompositeVideoRepository
import com.example.videoplayer.data.repository.VideoRepository
import com.example.videoplayer.databinding.ActivityPlayerBinding
import com.example.videoplayer.player.PlaybackService
import com.example.videoplayer.player.PlayerManager
import com.example.videoplayer.ui.main.MainViewModel
import com.example.videoplayer.ui.main.SettingsBottomSheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {
    companion object {
        private const val ACTION_PIP_CONTROL = "com.example.videoplayer.PIP_CONTROL"
        private const val EXTRA_PIP_CONTROL = "control"
        private const val PIP_CONTROL_PLAY_PAUSE = 1
        private const val PIP_CONTROL_NEXT = 2
        private const val PIP_CONTROL_PREVIOUS = 3
    }

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var playerManager: PlayerManager
    private lateinit var videoRepository: VideoRepository
    private var videoList: List<VideoFile> = emptyList()
    private var currentIndex: Int = -1
    private var folderUri: String? = null
    private var currentFileName: String? = null
    private var currentVideoUri: Uri? = null
    private var isBackgroundPlayEnabled: Boolean = false
    private var repeatMode: Int = Player.REPEAT_MODE_OFF
    private var isShuffleEnabled: Boolean = false
    private var isAutoPipEnabled: Boolean = true
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideControls() }
    private val HIDE_DELAY = 3000L
    private lateinit var resumeManager: ResumeManager
    private var playJob: Job? = null
    private var isLocked = false
    private var isFastForwarding = false
    private var originalSpeed = 1.0f
    
    private var initialVolume: Int = 0
    private var initialBrightness: Float = 0f

    private var abLoopA: Long = -1L
    private var abLoopB: Long = -1L
    private enum class ABLoopState { OFF, SET_A, SET_B }
    private var abLoopState = ABLoopState.OFF

    private var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerManager = PlayerManager(this)
        videoRepository = CompositeVideoRepository(this)
        resumeManager = ResumeManager(this)
        
        binding.playerView.player = playerManager.player
        binding.playerView.useController = false
        binding.playerView.keepScreenOn = true

        applySystemBarInsets()
        setupSettingsObservers()
        handleIntent()
        setupControls()
        setupGestures()
        setupPlaybackButtonAnimations()
        
        playerManager.player.addListener(playerListener)
        androidx.core.content.ContextCompat.registerReceiver(
            this, pipActionReceiver, IntentFilter(ACTION_PIP_CONTROL),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // 起動直後に表示されているコントロールも自動で隠す / Auto-hide the controls shown at launch too
        hideHandler.postDelayed(hideRunnable, HIDE_DELAY)
    }

    // エッジツーエッジ表示でステータスバー/ナビゲーションバーにコントロールが重ならないようにする
    // Keep the top/bottom bars clear of the status/navigation bars in edge-to-edge mode
    private fun applySystemBarInsets() {
        val top = binding.controlsLayout
        val bottom = binding.bottomControls
        val topPad = intArrayOf(top.paddingLeft, top.paddingTop, top.paddingRight)
        val bottomPad = intArrayOf(bottom.paddingLeft, bottom.paddingRight, bottom.paddingBottom)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            top.setPadding(topPad[0] + bars.left, topPad[1] + bars.top, topPad[2] + bars.right, top.paddingBottom)
            bottom.setPadding(bottomPad[0] + bars.left, bottom.paddingTop, bottomPad[1] + bars.right, bottomPad[2] + bars.bottom)
            insets
        }
    }

    private fun setupSettingsObservers() {
        lifecycleScope.launch {
            viewModel.playbackSpeed.collect { playerManager.speed = it }
        }
        lifecycleScope.launch {
            viewModel.isBackgroundPlayEnabled.collect { isBackgroundPlayEnabled = it }
        }
        lifecycleScope.launch {
            viewModel.isAutoPipEnabled.collect {
                isAutoPipEnabled = it
                updatePipParams()
            }
        }
        // プレイヤーには1ファイルずつ渡すため、全曲リピート/シャッフルはplayNext()で処理する
        // The player holds one item at a time, so repeat-all/shuffle are handled in playNext()
        lifecycleScope.launch {
            viewModel.repeatMode.collect {
                repeatMode = it
                playerManager.player.repeatMode =
                    if (it == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            }
        }
        lifecycleScope.launch {
            viewModel.shuffleModeEnabled.collect { isShuffleEnabled = it }
        }
        lifecycleScope.launch {
            viewModel.stopPlaybackEvent.collect { finish() }
        }
    }

    private fun setupPlaybackButtonAnimations() {
        val buttons = listOf(binding.btnPlayPause, binding.btnNext, binding.btnPrevious)
        buttons.forEach { button ->
            button.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    }
                }
                false
            }
        }
    }

    private fun handleIntent() {
        val videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("video_uri", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>("video_uri")
        }
        folderUri = intent.getStringExtra("folder_uri")
        currentFileName = videoUri?.lastPathSegment
        currentVideoUri = videoUri

        folderUri?.let { uriString ->
            lifecycleScope.launch {
                videoList = videoRepository.getVideoFiles(Uri.parse(uriString))
                currentIndex = videoList.indexOfFirst { it.uri == videoUri }
                if (currentIndex != -1) {
                    playVideo(currentIndex)
                } else if (videoUri != null) {
                    val pos = resumeManager.getFileResumePosition(videoUri.toString())
                    playerManager.play(videoUri, pos)
                }
            }
        } ?: run {
            videoUri?.let { 
                val pos = resumeManager.getFileResumePosition(it.toString())
                playerManager.play(it, pos) 
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                saveCurrentPosition()
                playNext()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayPauseIcon(isPlaying)
            updatePipParams()
        }

        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            updatePipParams()
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            checkABLoop()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                checkABLoop()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.widget.Toast.makeText(this@PlayerActivity, getString(R.string.playback_error), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private val abLoopHandler = Handler(Looper.getMainLooper())
    private val abLoopRunnable = object : Runnable {
        override fun run() {
            checkABLoop()
            abLoopHandler.postDelayed(this, 500)
        }
    }

    private fun checkABLoop() {
        if (abLoopA != -1L && abLoopB != -1L) {
            val current = playerManager.player.currentPosition
            if (current >= abLoopB || current < abLoopA) {
                playerManager.player.seekTo(abLoopA)
            }
        }
    }

    private fun playVideo(index: Int) {
        if (index < 0 || index >= videoList.size) return

        // 切り替え前に現在の動画の位置を保存（バックグラウンド連続再生でもレジュームを維持）
        // Save the outgoing video's position so resume survives continuous background playback
        if (currentIndex != index) saveCurrentPosition()

        val video = videoList[index]
        currentIndex = index
        currentVideoUri = video.uri
        currentFileName = video.name
        
        playJob?.cancel()
        playJob = lifecycleScope.launch {
            val folder = folderUri?.let { Uri.parse(it) }
            val subtitleConfigs = if (folder != null) {
                videoRepository.getSubtitleFiles(folder, video.name).map { createSubtitleConfig(it) }
            } else emptyList()

            val savedPosition = resumeManager.getFileResumePosition(video.uri.toString())
            val savedDuration = resumeManager.getFileDuration(video.uri.toString())
            // 視聴済み（終端付近）の動画は先頭から / Restart videos that were watched to the end
            val position = if (savedDuration > 0 && savedPosition >= savedDuration - 3000) 0L else savedPosition
            playerManager.play(video, subtitleConfigs, position)
            updateFileNameDisplay()
            
            // Reset AB Loop
            abLoopA = -1L
            abLoopB = -1L
            abLoopState = ABLoopState.OFF
            updateABLoopButtonUI()
        }
    }

    private fun createSubtitleConfig(uri: Uri): MediaItem.SubtitleConfiguration {
        val extension = uri.toString().substringAfterLast('.', "").lowercase()
        val mimeType = when (extension) {
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            "vtt" -> MimeTypes.TEXT_VTT
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }
        return MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLanguage("und")
            .setLabel(uri.lastPathSegment ?: "Subtitle")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
    }

    private fun setupControls() {
        binding.btnBack.setOnClickListener { if (!isLocked) finish() }
        binding.tvFileName.text = currentFileName

        binding.btnSubtitles.setOnClickListener {
            if (isLocked) return@setOnClickListener
            showTrackSelectionDialog(C.TRACK_TYPE_TEXT, "Select Subtitles")
        }

        binding.btnLock.setOnClickListener {
            toggleLock()
        }

        binding.btnPip.setOnClickListener {
            if (isLocked) return@setOnClickListener
            enterPip()
        }

        binding.btnSettings.setOnClickListener {
            if (isLocked) return@setOnClickListener
            SettingsBottomSheet().show(supportFragmentManager, "settings")
        }

        binding.btnPlayPause.setOnClickListener {
            if (isLocked) return@setOnClickListener
            if (playerManager.player.isPlaying) {
                playerManager.player.pause()
            } else {
                playerManager.player.play()
            }
        }

        binding.btnNext.setOnClickListener {
            if (isLocked) return@setOnClickListener
            playNext()
        }

        binding.btnPrevious.setOnClickListener {
            if (isLocked) return@setOnClickListener
            playPrevious()
        }

        binding.btnAspectRatio.setOnClickListener {
            if (isLocked) return@setOnClickListener
            cycleAspectRatio()
        }

        binding.btnAbLoop.setOnClickListener {
            if (isLocked) return@setOnClickListener
            cycleABLoop()
        }
    }

    private fun toggleLock() {
        isLocked = !isLocked
        binding.btnLock.setImageResource(if (isLocked) R.drawable.ic_lock_v_closed else R.drawable.ic_lock_v)
        updatePipParams()
        if (isLocked) {
            hideControls()
            android.widget.Toast.makeText(this, "Screen Locked", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            showControls()
            android.widget.Toast.makeText(this, "Screen Unlocked", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTrackSelectionDialog(trackType: Int, title: String) {
        androidx.media3.ui.TrackSelectionDialogBuilder(this, title, playerManager.player, trackType)
            .build()
            .show()
    }

    private fun playNext() {
        if (videoList.isEmpty()) return
        val next = when {
            isShuffleEnabled && videoList.size > 1 ->
                videoList.indices.filter { it != currentIndex }.random()
            currentIndex < videoList.size - 1 -> currentIndex + 1
            repeatMode == Player.REPEAT_MODE_ALL -> 0
            else -> -1
        }
        if (next >= 0) {
            playVideo(next)
        } else {
            android.widget.Toast.makeText(this, getString(R.string.last_file), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun playPrevious() {
        if (currentIndex > 0) {
            playVideo(currentIndex - 1)
        } else if (repeatMode == Player.REPEAT_MODE_ALL && videoList.isNotEmpty()) {
            playVideo(videoList.size - 1)
        } else {
            android.widget.Toast.makeText(this, getString(R.string.first_file), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFileNameDisplay() {
        binding.tvFileName.text = currentFileName ?: "Unknown"
    }

    private fun cycleAspectRatio() {
        resizeMode = when (resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        binding.playerView.resizeMode = resizeMode
        val modeText = when (resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill"
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> "Fixed Width"
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> "Fixed Height"
            else -> "Fit"
        }
        showIndicator(R.drawable.ic_aspect_ratio_v, -1, modeText)
    }

    private fun cycleABLoop() {
        val currentPos = playerManager.player.currentPosition
        when (abLoopState) {
            ABLoopState.OFF -> {
                abLoopA = currentPos
                abLoopState = ABLoopState.SET_A
                showIndicator(R.drawable.ic_loop_v, -1, "A: ${formatTime(abLoopA)}")
            }
            ABLoopState.SET_A -> {
                if (currentPos > abLoopA) {
                    abLoopB = currentPos
                    abLoopState = ABLoopState.SET_B
                    showIndicator(R.drawable.ic_loop_v, -1, "B: ${formatTime(abLoopB)} (Loop ON)")
                } else {
                    android.widget.Toast.makeText(this, "B must be after A", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            ABLoopState.SET_B -> {
                abLoopA = -1L
                abLoopB = -1L
                abLoopState = ABLoopState.OFF
                showIndicator(R.drawable.ic_loop_v, -1, "Loop OFF")
            }
        }
        updateABLoopButtonUI()
    }

    private fun updateABLoopButtonUI() {
        val color = if (abLoopState != ABLoopState.OFF) getColor(R.color.vivid_blue) else 0xFFFFFFFF.toInt()
        binding.btnAbLoop.setColorFilter(color)
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60))
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun showControls() {
        binding.controlsLayout.visibility = View.VISIBLE
        binding.bottomControls.visibility = View.VISIBLE
        showSystemBars()
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, HIDE_DELAY)
    }

    private fun hideControls() {
        binding.controlsLayout.visibility = View.GONE
        binding.bottomControls.visibility = View.GONE
        hideSystemBars()
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && binding.controlsLayout.visibility == View.GONE) {
            hideSystemBars()
        }
    }

    private fun setupGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                // スクロール開始時の値を記憶して安定させる
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                initialBrightness = window.attributes.screenBrightness
                if (initialBrightness < 0) initialBrightness = 0.5f // システムデフォルトの場合の暫定値
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isLocked || e1 == null) return false
                
                // 開始点(e1)からの累積移動距離を使用
                val deltaY = e1.y - e2.y
                val height = binding.playerView.height.toFloat()
                val screenWidth = binding.playerView.width.toFloat()
                
                if (e2.x < screenWidth / 2) {
                    // 左側: 輝度調整 (画面の高さ分スワイプで 0.0 -> 1.0)
                    val brightnessDelta = deltaY / height
                    val newBrightness = (initialBrightness + brightnessDelta).coerceIn(0.01f, 1.0f)
                    val lp = window.attributes
                    lp.screenBrightness = newBrightness
                    window.attributes = lp
                    showIndicator(R.drawable.ic_brightness, (newBrightness * 100).toInt())
                } else {
                    // 右側: 音量調整 (画面の高さ分スワイプで 0 -> MaxVolume)
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val volumeDelta = ((deltaY / height) * maxVolume).toInt()
                    val newVolume = (initialVolume + volumeDelta).coerceIn(0, maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                    showIndicator(R.drawable.ic_volume, (newVolume.toFloat() / maxVolume * 100).toInt())
                }
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (binding.controlsLayout.visibility == View.VISIBLE) hideControls() else showControls()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (!isLocked) startFastForward()
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isLocked) return false
                val isLeft = e.x < binding.playerView.width / 2
                if (isLeft) playerManager.player.seekBack() else playerManager.player.seekForward()
                showIndicator(if (isLeft) R.drawable.ic_previous else R.drawable.ic_next, -1, if (isLeft) "-10s" else "+10s")
                return true
            }
        })

        binding.playerView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                if (isFastForwarding) stopFastForward()
                if (binding.indicatorLayout.visibility == View.VISIBLE) {
                    hideHandler.postDelayed({ binding.indicatorLayout.visibility = View.GONE }, 1000)
                }
            }
            true
        }
    }

    private fun startFastForward() {
        isFastForwarding = true
        originalSpeed = playerManager.player.playbackParameters.speed
        playerManager.player.setPlaybackSpeed(2.0f)
        binding.tvSpeedIndicator.visibility = View.VISIBLE
        hideControls()
    }

    private fun stopFastForward() {
        isFastForwarding = false
        playerManager.player.setPlaybackSpeed(originalSpeed)
        binding.tvSpeedIndicator.visibility = View.GONE
    }

    private fun showIndicator(iconRes: Int, progress: Int, text: String? = null) {
        binding.indicatorLayout.visibility = View.VISIBLE
        binding.ivIndicatorIcon.setImageResource(iconRes)
        if (text != null) {
            binding.tvIndicatorText.visibility = View.VISIBLE
            binding.tvIndicatorText.text = text
        } else {
            binding.tvIndicatorText.visibility = View.GONE
        }
        if (progress >= 0) {
            binding.pbIndicator.visibility = View.VISIBLE
            binding.pbIndicator.progress = progress
        } else {
            binding.pbIndicator.visibility = View.GONE
        }
    }

    // ---- ピクチャー・イン・ピクチャー / Picture-in-Picture ----

    private val hasPipFeature by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    // PiPウィンドウのボタン（前へ/再生・一時停止/次へ）からのブロードキャストを受ける
    // Receives taps on the PiP window's Previous / Play-Pause / Next buttons
    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(EXTRA_PIP_CONTROL, 0)) {
                PIP_CONTROL_PLAY_PAUSE ->
                    if (playerManager.player.isPlaying) playerManager.player.pause() else playerManager.player.play()
                PIP_CONTROL_NEXT -> playNext()
                PIP_CONTROL_PREVIOUS -> playPrevious()
            }
        }
    }

    private fun pipAction(control: Int, iconRes: Int, titleRes: Int): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL).setPackage(packageName).putExtra(EXTRA_PIP_CONTROL, control)
        val pendingIntent = PendingIntent.getBroadcast(this, control, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val title = getString(titleRes)
        return RemoteAction(Icon.createWithResource(this, iconRes), title, title, pendingIntent)
    }

    private fun buildPipParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
        val size = playerManager.player.videoSize
        if (size.width > 0 && size.height > 0) {
            // PiPのアスペクト比は 1:2.39〜2.39:1 の範囲でないと例外になる
            // PiP rejects aspect ratios outside 1:2.39 .. 2.39:1
            val ratio = (size.width * size.pixelWidthHeightRatio / size.height).coerceIn(1 / 2.39f, 2.39f)
            builder.setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
        }
        val isPlaying = playerManager.player.isPlaying
        builder.setActions(listOf(
            pipAction(PIP_CONTROL_PREVIOUS, R.drawable.ic_previous, R.string.pip_previous),
            pipAction(PIP_CONTROL_PLAY_PAUSE, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play, R.string.pip_play_pause),
            pipAction(PIP_CONTROL_NEXT, R.drawable.ic_next, R.string.pip_next)
        ))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12以降: 再生中にホームへ戻ると自動でスムーズにPiPへ移行
            // Android 12+: seamlessly auto-enter PiP when going Home while playing
            builder.setAutoEnterEnabled(isAutoPipEnabled && isPlaying && !isLocked)
        }
        return builder.build()
    }

    private fun updatePipParams() {
        if (!hasPipFeature || !::playerManager.isInitialized) return
        setPictureInPictureParams(buildPipParams())
    }

    private fun enterPip() {
        if (!hasPipFeature) {
            android.widget.Toast.makeText(this, R.string.pip_not_supported, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        hideControls()
        enterPictureInPictureMode(buildPipParams())
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 11以前は自動移行がないため、ここで手動でPiPに入る
        // Android 11 and below have no auto-enter, so enter PiP manually here
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && hasPipFeature &&
            isAutoPipEnabled && playerManager.player.isPlaying && !isLocked) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // PiP中は小窓なのでオーバーレイ類をすべて隠す / Hide all overlays in the small PiP window
            hideHandler.removeCallbacks(hideRunnable)
            hideControls()
            binding.indicatorLayout.visibility = View.GONE
            binding.tvSpeedIndicator.visibility = View.GONE
            (supportFragmentManager.findFragmentByTag("settings") as? SettingsBottomSheet)?.dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.playerView.player = playerManager.player
        abLoopHandler.post(abLoopRunnable)
    }

    override fun onStop() {
        super.onStop()
        abLoopHandler.removeCallbacks(abLoopRunnable)
        // 戻るボタン/おやすみタイマーで終了する場合はバックグラウンド再生しない
        // Don't keep playing when the user (or the sleep timer) is closing the player
        // PiP中のonStopは「PiPウィンドウが閉じられた」ことを意味するので、PiPを理由に再生継続しない
        // onStop while in PiP means the PiP window was dismissed, so PiP alone is no reason to keep playing
        val keepPlaying = !isFinishing && isBackgroundPlayEnabled
        // MediaSessionServiceは再生中でないとstartForeground()しないため、停止中に起動するとクラッシュする
        // MediaSessionService only calls startForeground() while playing; starting it while paused crashes
        if (keepPlaying && playerManager.player.playWhenReady) {
            val intent = Intent(this, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        } else {
            playerManager.player.pause()
            stopService(Intent(this, PlaybackService::class.java))
        }
        binding.playerView.player = null
    }

    override fun onPause() {
        super.onPause()
        saveCurrentPosition()
    }

    private fun saveCurrentPosition() {
        val uri = folderUri ?: return
        val fileName = currentFileName ?: return
        val videoUri = currentVideoUri ?: return
        val duration = playerManager.player.duration.takeIf { it > 0 } ?: 0L
        resumeManager.saveResumePosition(uri, fileName, videoUri.toString(), playerManager.player.currentPosition, duration)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.player.removeListener(playerListener)
        unregisterReceiver(pipActionReceiver)
        if (isFinishing || !isBackgroundPlayEnabled) {
            stopService(Intent(this, PlaybackService::class.java))
            playerManager.release()
        }
    }
}
