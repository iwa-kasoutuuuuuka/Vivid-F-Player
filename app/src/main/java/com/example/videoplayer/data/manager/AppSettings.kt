package com.example.videoplayer.data.manager

import android.content.Context
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * アプリ全体で共有する再生設定。どの画面で変更しても、開いている全画面に即座に反映されます。
 * App-wide playback settings shared by every screen, so a change made anywhere reaches
 * screens that are already open (each activity has its own ViewModel).
 */
class AppSettings private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isBackgroundPlayEnabled = MutableStateFlow(prefs.getBoolean("bg_play", false))
    val isBackgroundPlayEnabled: StateFlow<Boolean> = _isBackgroundPlayEnabled.asStateFlow()

    private val _isAutoPipEnabled = MutableStateFlow(prefs.getBoolean("auto_pip", true))
    val isAutoPipEnabled: StateFlow<Boolean> = _isAutoPipEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(prefs.getFloat("playback_speed", 1.0f))
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(prefs.getInt("repeat_mode", Player.REPEAT_MODE_OFF))
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(prefs.getBoolean("shuffle_mode", false))
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _skipSeconds = MutableStateFlow(prefs.getInt("skip_seconds", 10))
    val skipSeconds: StateFlow<Int> = _skipSeconds.asStateFlow()

    private val _longPressSpeed = MutableStateFlow(prefs.getFloat("long_press_speed", 2.0f))
    val longPressSpeed: StateFlow<Float> = _longPressSpeed.asStateFlow()

    private val _isVoiceBoostEnabled = MutableStateFlow(prefs.getBoolean("voice_boost", false))
    val isVoiceBoostEnabled: StateFlow<Boolean> = _isVoiceBoostEnabled.asStateFlow()

    private val _isNightModeEnabled = MutableStateFlow(prefs.getBoolean("night_mode", false))
    val isNightModeEnabled: StateFlow<Boolean> = _isNightModeEnabled.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    private val _stopPlaybackEvent = MutableSharedFlow<Unit>()
    val stopPlaybackEvent: SharedFlow<Unit> = _stopPlaybackEvent.asSharedFlow()

    private var sleepTimerJob: Job? = null

    fun setBackgroundPlayEnabled(enabled: Boolean) {
        _isBackgroundPlayEnabled.value = enabled
        prefs.edit().putBoolean("bg_play", enabled).apply()
    }

    fun setAutoPipEnabled(enabled: Boolean) {
        _isAutoPipEnabled.value = enabled
        prefs.edit().putBoolean("auto_pip", enabled).apply()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        prefs.edit().putFloat("playback_speed", speed).apply()
    }

    fun setRepeatMode(mode: Int) {
        _repeatMode.value = mode
        prefs.edit().putInt("repeat_mode", mode).apply()
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        _shuffleModeEnabled.value = enabled
        prefs.edit().putBoolean("shuffle_mode", enabled).apply()
    }

    fun setSkipSeconds(seconds: Int) {
        _skipSeconds.value = seconds
        prefs.edit().putInt("skip_seconds", seconds).apply()
    }

    fun setLongPressSpeed(speed: Float) {
        _longPressSpeed.value = speed
        prefs.edit().putFloat("long_press_speed", speed).apply()
    }

    fun setVoiceBoostEnabled(enabled: Boolean) {
        _isVoiceBoostEnabled.value = enabled
        prefs.edit().putBoolean("voice_boost", enabled).apply()
    }

    fun setNightModeEnabled(enabled: Boolean) {
        _isNightModeEnabled.value = enabled
        prefs.edit().putBoolean("night_mode", enabled).apply()
    }

    /**
     * おやすみタイマー。画面に紐付かないスコープで動くため、一覧画面で設定しても再生画面を停止できます。
     * Sleep timer; runs outside any screen's scope so a timer set from the list screen still stops the player.
     */
    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = scope.launch {
                delay(minutes * 60 * 1000L)
                _sleepTimerMinutes.value = 0
                _stopPlaybackEvent.emit(Unit)
            }
        }
    }

    companion object {
        @Volatile private var instance: AppSettings? = null

        fun getInstance(context: Context): AppSettings =
            instance ?: synchronized(this) {
                instance ?: AppSettings(context).also { instance = it }
            }
    }
}
