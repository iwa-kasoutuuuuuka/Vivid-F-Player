package com.example.videoplayer.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Player
import com.example.videoplayer.data.manager.ResumeManager
import com.example.videoplayer.databinding.ActivityPlayerBinding
import com.example.videoplayer.player.PlayerManager
import com.example.videoplayer.data.repository.FileRepository
import com.example.videoplayer.data.model.VideoFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.net.Uri
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.util.Rational

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
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

        val videoUri = intent.getParcelableExtra<Uri>("video_uri")
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

        playerManager.onError = {
            android.widget.Toast.makeText(this, "再生エラー: ファイルを再生できません", android.widget.Toast.LENGTH_SHORT).show()
        }

        playerManager.player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    playNext()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon(isPlaying)
            }
        })

        setupControls()
        setupGestures()
    }

    override fun onUserLeaveHint() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(binding.playerView.width, binding.playerView.height))
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
            binding.playerView.useController = true
        }
    }

    private fun updateFileNameDisplay() {
        binding.tvFileName.text = currentFileName ?: "Unknown"
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) com.example.videoplayer.R.drawable.ic_pause 
            else com.example.videoplayer.R.drawable.ic_play
        )
    }

    private fun playNext() {
        if (videoList.isEmpty()) return
        if (currentIndex < videoList.size - 1) {
            currentIndex++
            playCurrentIndex()
        } else {
            android.widget.Toast.makeText(this, "最後のファイルです", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun playPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            playCurrentIndex()
        } else {
            android.widget.Toast.makeText(this, "最初のファイルです", android.widget.Toast.LENGTH_SHORT).show()
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
            val settings = com.example.videoplayer.ui.main.SettingsBottomSheet().apply {
                this.currentSpeed = playerManager.speed
                this.isBackgroundPlayEnabled = this@PlayerActivity.isBackgroundPlayEnabled
                this.onSpeedChanged = { speed ->
                    playerManager.speed = speed
                }
                this.onBackgroundPlayChanged = { enabled ->
                    this@PlayerActivity.isBackgroundPlayEnabled = enabled
                }
            }
            settings.show(supportFragmentManager, "settings")
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
        
        // 初期状態でタイマー開始
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
        val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                showControls() // 操作中は表示
                if (Math.abs(distanceX) > Math.abs(distanceY)) {
                    // 左右スワイプ：シーク
                    val seekAmount = if (distanceX > 0) -5000L else 5000L
                    playerManager.player.seekTo(playerManager.player.currentPosition + seekAmount)
                } else {
                    // 上下スワイプ：明るさ/音量
                    if (e2.x < binding.root.width / 2) {
                        // 左側：明るさ調整
                        val lp = window.attributes
                        lp.screenBrightness = (lp.screenBrightness + (if (distanceY > 0) 0.1f else -0.1f)).coerceIn(0f, 1f)
                        window.attributes = lp
                    } else {
                        // 右側：音量調整
                        val direction = if (distanceY > 0) android.media.AudioManager.ADJUST_RAISE else android.media.AudioManager.ADJUST_LOWER
                        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, android.media.AudioManager.FLAG_SHOW_UI)
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
        })

        binding.playerView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                // UI非表示タイマーのリセットなどをここで行うことが可能
            }
            v.performClick()
            true
        }
    }

    override fun onStart() {
        super.onStart()
        binding.playerView.player = playerManager.player
    }

    override fun onStop() {
        super.onStop()
        if (!isBackgroundPlayEnabled && !isInPictureInPictureMode) {
            playerManager.player.pause()
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
        playerManager.release()
    }
}
