package com.example.videoplayer.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

object PlayerHolder {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    fun getPlayer(context: Context): ExoPlayer {
        if (player == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
                .build()

            player = ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(audioAttributes, true) // true handles audio focus automatically
                .setHandleAudioBecomingNoisy(true) // Pauses when headphones are unplugged
                .setWakeMode(C.WAKE_MODE_LOCAL) // Keeps CPU awake during playback
                .build()
        }
        return player!!
    }

    fun getMediaSession(context: Context): MediaSession {
        if (mediaSession == null) {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            mediaSession = MediaSession.Builder(context.applicationContext, getPlayer(context))
                .setSessionActivity(pendingIntent)
                .build()
        }
        return mediaSession!!
    }

    fun release() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
    }
}
