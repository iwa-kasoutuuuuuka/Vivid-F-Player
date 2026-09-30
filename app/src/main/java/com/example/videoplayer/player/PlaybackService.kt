package com.example.videoplayer.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "VividFPlayer:PlaybackWakeLock")
        // MediaControllerが接続しなくても通知(startForeground)が出るようにセッションを登録
        // Register the session up front; otherwise no controller connects, no notification is posted,
        // and startForegroundService() times out (ForegroundServiceDidNotStartInTimeException)
        addSession(PlayerHolder.getMediaSession(this))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return PlayerHolder.getMediaSession(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        // プレイヤーの解放はPlayerActivityが行う（ここで解放すると画面復帰時に再生不能になる）
        // PlayerActivity owns the player's release; releasing here breaks the still-open player screen
        PlayerHolder.currentMediaSession()?.let { removeSession(it) }
        super.onDestroy()
    }
}
