package com.example.videoplayer.ui.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
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
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideControls() }
    private val HIDE_DELAY = 3000L
    private lateinit var resumeManager: ResumeManager
    private var playJob: Job? = null
    private var isLocked = false
    private var isFastForwarding = false
    private var originalSpeed = 1.0f

    private var abLoopA: Long = -1L
    private var abLoopB: Long = -1L
    private enum class ABLoopState { OFF, SET_A, SET_B }
    private var abLoopState = ABLoopState.OFF

    private var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerManager = PlayerManager(this)
        videoRepository = CompositeVideoRepository(this)
        resumeManager = ResumeManager(this)
        
        binding.playerView.player = playerManager.player
        binding.playerView.useController = false
        binding.playerView.keepScreenOn = true

        setupSettingsObservers()
        handleIntent()
        setupControls()
        setupGestures()
        
        playerManager.player.addListener(playerListener)
    }

    private fun setupSettingsObservers() {
        lifecycleScope.launch {
            viewModel.playbackSpeed.collect { playerManager.speed = it }
        }
        lifecycleScope.launch {
            viewModel.isBackgroundPlayEnabled.collect { isBackgroundPlayEnabled = it }
        }
        lifecycleScope.launch {
            viewModel.repeatMode.collect { playerManager.player.repeatMode = it }
        }
        lifecycleScope.launch {
            viewModel.shuffleModeEnabled.collect { playerManager.player.shuffleModeEnabled = it }
        }
        lifecycleScope.launch {
            viewModel.stopPlaybackEvent.collect { finish() }
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
                playNext()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayPauseIcon(isPlaying)
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

            val position = resumeManager.getFileResumePosition(video.uri.toString())
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
        
        // Bonus: Aspect Ratio Toggle
        binding.playerView.setOnClickListener {
            if (isLocked) {
                toggleLock() // Easy unlock on click? No, maybe just show controls
            } else {
                if (binding.controlsLayout.visibility == View.VISIBLE) hideControls() else showControls()
            }
        }
    }

    private fun toggleLock() {
        isLocked = !isLocked
        binding.btnLock.setImageResource(if (isLocked) R.drawable.ic_lock else R.drawable.ic_lock_open)
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
        if (currentIndex < videoList.size - 1) {
            playVideo(currentIndex + 1)
        } else {
            android.widget.Toast.makeText(this, getString(R.string.last_file), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun playPrevious() {
        if (currentIndex > 0) {
            playVideo(currentIndex - 1)
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
        showIndicator(R.drawable.ic_aspect_ratio, -1, modeText)
    }

    private fun cycleABLoop() {
        val currentPos = playerManager.player.currentPosition
        when (abLoopState) {
            ABLoopState.OFF -> {
                abLoopA = currentPos
                abLoopState = ABLoopState.SET_A
                showIndicator(R.drawable.ic_loop, -1, "A: ${formatTime(abLoopA)}")
            }
            ABLoopState.SET_A -> {
                if (currentPos > abLoopA) {
                    abLoopB = currentPos
                    abLoopState = ABLoopState.SET_B
                    showIndicator(R.drawable.ic_loop, -1, "B: ${formatTime(abLoopB)} (Loop ON)")
                } else {
                    android.widget.Toast.makeText(this, "B must be after A", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            ABLoopState.SET_B -> {
                abLoopA = -1L
                abLoopB = -1L
                abLoopState = ABLoopState.OFF
                showIndicator(R.drawable.ic_loop, -1, "Loop OFF")
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
        hideHandler.removeCallbacks(hideRunnable)
        if (!isLocked) {
            hideHandler.postDelayed(hideRunnable, HIDE_DELAY)
        }
    }

    private fun hideControls() {
        binding.controlsLayout.visibility = View.GONE
        binding.bottomControls.visibility = View.GONE
    }

    private fun setupGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isLocked) return false
                val deltaY = (e1?.y ?: 0f) - e2.y
                val screenWidth = binding.playerView.width
                if (e2.x < screenWidth / 2) {
                    // Brightness
                    val lp = window.attributes
                    lp.screenBrightness = (lp.screenBrightness + deltaY / 1000).coerceIn(0.01f, 1.0f)
                    window.attributes = lp
                    showIndicator(R.drawable.ic_brightness, (lp.screenBrightness * 100).toInt())
                } else {
                    // Volume
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val deltaVol = (deltaY / 500).toInt()
                    if (deltaVol != 0) {
                        val newVolume = (currentVolume + (if (deltaVol > 0) 1 else -1)).coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                        showIndicator(R.drawable.ic_volume, (newVolume.toFloat() / maxVolume * 100).toInt())
                    }
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
            if (isLocked) {
                if (event.action == MotionEvent.ACTION_DOWN && binding.controlsLayout.visibility != View.VISIBLE) {
                    showControls()
                }
                return@setOnTouchListener false
            }
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                if (isFastForwarding) stopFastForward()
                if (binding.indicatorLayout.visibility == View.VISIBLE) {
                    hideHandler.postDelayed({ binding.indicatorLayout.visibility = View.GONE }, 1000)
                }
            }
            v.performClick()
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

    override fun onStart() {
        super.onStart()
        binding.playerView.player = playerManager.player
        abLoopHandler.post(abLoopRunnable)
    }

    override fun onStop() {
        super.onStop()
        abLoopHandler.removeCallbacks(abLoopRunnable)
        if (isBackgroundPlayEnabled || isInPictureInPictureMode) {
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
        folderUri?.let { uri ->
            currentFileName?.let { fileName ->
                val duration = playerManager.player.duration
                if (duration > 0) {
                    resumeManager.saveResumePosition(uri, fileName, currentVideoUri.toString(), playerManager.player.currentPosition, duration)
                } else {
                    resumeManager.saveResumePosition(uri, fileName, currentVideoUri.toString(), playerManager.player.currentPosition, 0)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.player.removeListener(playerListener)
        if (!isBackgroundPlayEnabled) playerManager.release()
    }
}
