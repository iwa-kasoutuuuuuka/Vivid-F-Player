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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import com.example.videoplayer.R
import com.example.videoplayer.data.manager.ResumeManager
import com.example.videoplayer.data.model.VideoFile
import com.example.videoplayer.data.repository.FileRepository
import com.example.videoplayer.databinding.ActivityPlayerBinding
import com.example.videoplayer.player.PlaybackService
import com.example.videoplayer.player.PlayerManager
import com.example.videoplayer.ui.main.MainViewModel
import com.example.videoplayer.ui.main.SettingsBottomSheet
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var playerManager: PlayerManager
    private lateinit var fileRepository: FileRepository
    private var videoList: List<VideoFile> = emptyList()
    private var currentIndex: Int = -1
    private var folderUri: String? = null
    private var currentFileName: String? = null
    private var currentVideoUri: Uri? = null
    private var isBackgroundPlayEnabled: Boolean = false
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideControls() }
    private val HIDE_DELAY = 3000L // 3秒

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerManager = PlayerManager(this)
        fileRepository = FileRepository(this)
        
        binding.playerView.player = playerManager.player
        binding.playerView.useController = false
        binding.playerView.keepScreenOn = true

        // Settings 監視
        lifecycleScope.launch {
            viewModel.playbackSpeed.collect { speed ->
                playerManager.speed = speed
            }
        }
        lifecycleScope.launch {
            viewModel.isBackgroundPlayEnabled.collect { enabled ->
                isBackgroundPlayEnabled = enabled
            }
        }
        lifecycleScope.launch {
            viewModel.repeatMode.collect { mode ->
                playerManager.player.repeatMode = mode
            }
        }
        lifecycleScope.launch {
            viewModel.shuffleModeEnabled.collect { enabled ->
                playerManager.player.shuffleModeEnabled = enabled
            }
        }
        lifecycleScope.launch {
            viewModel.stopPlaybackEvent.collect {
                finish()
            }
        }

        val videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("video_uri", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>("video_uri")
        }
        folderUri = intent.getStringExtra("folder_uri")
        val resumePos = intent.getLongExtra("resume_pos", 0L)
        currentFileName = videoUri?.lastPathSegment

        folderUri?.let { uriString ->
            lifecycleScope.launch {
                videoList = fileRepository.getVideoFiles(Uri.parse(uriString))
                currentIndex = videoList.indexOfFirst { it.uri == videoUri }
                updateFileNameDisplay()
            }
        }

        currentVideoUri = videoUri
        videoUri?.let { playerManager.play(it, resumePos) }

        playerManager.player.addListener(playerListener)

        setupControls()
        setupGestures()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            // 自動連続再生 / Auto continuous playback
            if (playbackState == Player.STATE_ENDED) {
                playNext()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // 再生・一時停止アイコンの更新 / Update play/pause icon
            updatePlayPauseIcon(isPlaying)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.widget.Toast.makeText(this@PlayerActivity, getString(R.string.playback_error), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val width = binding.playerView.width.takeIf { it > 0 } ?: 16
            val height = binding.playerView.height.takeIf { it > 0 } ?: 9
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(width, height))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.controlsLayout.visibility = View.GONE
            binding.bottomControls.visibility = View.GONE
            binding.playerView.useController = false
        } else {
            binding.controlsLayout.visibility = View.VISIBLE
            binding.bottomControls.visibility = View.VISIBLE
            binding.playerView.useController = false
        }
    }

    private fun updateFileNameDisplay() {
        binding.tvFileName.text = currentFileName ?: "Unknown"
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause 
            else R.drawable.ic_play
        )
    }

    private fun playNext() {
        if (videoList.isEmpty()) return
        if (currentIndex < videoList.size - 1) {
            currentIndex++
            playCurrentIndex()
        } else {
            android.widget.Toast.makeText(this, getString(R.string.last_file), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun playPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            playCurrentIndex()
        } else {
            android.widget.Toast.makeText(this, getString(R.string.first_file), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun playCurrentIndex() {
        val video = videoList[currentIndex]
        currentFileName = video.name
        currentVideoUri = video.uri
        updateFileNameDisplay()
        playerManager.play(video.uri)
    }

    private fun setupControls() {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvFileName.text = currentFileName
        binding.tvFileName.isSelected = true // Enable marquee

        binding.btnSettings.setOnClickListener {
            resetHideTimer()
            SettingsBottomSheet().show(supportFragmentManager, "settings")
        }

        binding.btnPlayPause.setOnClickListener {
            resetHideTimer()
            if (playerManager.player.isPlaying) {
                playerManager.player.pause()
            } else {
                playerManager.player.play()
            }
        }

        binding.btnNext.setOnClickListener {
            resetHideTimer()
            playNext()
        }

        binding.btnPrevious.setOnClickListener {
            resetHideTimer()
            playPrevious()
        }
        
        resetHideTimer()
    }
    
    private fun showControls() {
        binding.controlsLayout.visibility = View.VISIBLE
        binding.bottomControls.visibility = View.VISIBLE
        resetHideTimer()
    }

    private fun hideControls() {
        if (isInPictureInPictureMode) return
        binding.controlsLayout.visibility = View.GONE
        binding.bottomControls.visibility = View.GONE
    }

    private fun resetHideTimer() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, HIDE_DELAY)
    }

    private fun setupGestures() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                showControls()
                val deltaY = (e1?.y ?: 0f) - e2.y
                val screenWidth = binding.playerView.width
                
                if (e2.x < screenWidth / 2) {
                    // Brightness (Left side)
                    val lp = window.attributes
                    val brightness = (lp.screenBrightness.takeIf { it >= 0 } ?: 0.5f) + deltaY / 2000f
                    lp.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
                    window.attributes = lp
                    showIndicator(R.drawable.ic_brightness, (lp.screenBrightness * 100).toInt())
                } else {
                    // Volume (Right side)
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
                if (binding.controlsLayout.visibility == View.VISIBLE) {
                    hideControls()
                } else {
                    showControls()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val screenWidth = binding.playerView.width
                if (e.x < screenWidth / 2) {
                    playerManager.player.seekBack()
                } else {
                    playerManager.player.seekForward()
                }
                return true
            }
        })

        binding.playerView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                // 指を離したらしばらくしてインジケーターを隠す
                hideHandler.postDelayed({ binding.indicatorLayout.visibility = View.GONE }, 1000)
            }
            v.performClick()
            true
        }
    }

    private fun showIndicator(iconRes: Int, progress: Int) {
        binding.indicatorLayout.visibility = View.VISIBLE
        binding.ivIndicatorIcon.setImageResource(iconRes)
        binding.pbIndicator.progress = progress
    }

    override fun onStart() {
        super.onStart()
        binding.playerView.player = playerManager.player
    }

    override fun onStop() {
        super.onStop()
        if (isBackgroundPlayEnabled || isInPictureInPictureMode) {
            val intent = Intent(this, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
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
                val videoUriStr = currentVideoUri?.toString() ?: ""
                ResumeManager(this).saveResumePosition(uri, fileName, videoUriStr, playerManager.player.currentPosition)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.player.removeListener(playerListener)
        // 不要なリークを防ぐため、バックグラウンド再生中でなければ解放
        // Release player if background playback is not enabled to prevent leaks
        if (!isBackgroundPlayEnabled) {
            playerManager.release()
        }
    }
}
